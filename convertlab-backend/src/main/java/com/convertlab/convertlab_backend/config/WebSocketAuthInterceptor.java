package com.convertlab.convertlab_backend.config;

import com.convertlab.convertlab_backend.security_util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intercepts STOMP frames to enforce authentication rules per destination:
 *
 * <pre>
 * /user/**          → requires valid JWT (user-specific messages)
 * /topic/session/** → open, no auth required (sessionId-based routing)
 * /topic/**         → open, no auth required (broadcasts)
 * </pre>
 *
 * The JWT is read from the native {@code Authorization: Bearer <token>} STOMP header,
 * which the frontend sends in the CONNECT frame (and optionally on SUBSCRIBE).
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // ── CONNECT: try to set principal from JWT (optional) ────────────────
        if (StompCommand.CONNECT.equals(command)) {
            String token = extractToken(accessor);
            if (token != null) {
                trySetPrincipal(accessor, token);
            }
            // No token → anonymous connection is fine; session-based subscriptions still work
        }

        // ── SUBSCRIBE: enforce auth only for /user/** destinations ───────────
        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/user/")) {
                // Make sure we have an authenticated principal
                if (accessor.getUser() == null) {
                    // Give it one more chance — token might be in the SUBSCRIBE header
                    String token = extractToken(accessor);
                    if (token != null) {
                        trySetPrincipal(accessor, token);
                    }
                }

                if (accessor.getUser() == null) {
                    log.warn("Rejected unauthenticated SUBSCRIBE to: {}", destination);
                    throw new IllegalArgumentException(
                            "Authentication required to subscribe to user-specific destinations"
                    );
                }

                log.debug("Authenticated SUBSCRIBE to {} by {}", destination, accessor.getUser().getName());
            }

            // /topic/session/{sessionId} and /topic/** → no auth check, allow through
        }

        return message;
    }

    private void trySetPrincipal(StompHeaderAccessor accessor, String token) {
        try {
            String email = jwtUtil.validateAccessTokenAndGetEmail(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            accessor.setUser(auth);
            log.debug("WebSocket principal set for: {}", email);
        } catch (Exception e) {
            log.debug("WebSocket JWT validation failed: {}", e.getMessage());
        }
    }

    /**
     * Reads the Bearer token from the STOMP native {@code Authorization} header.
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
