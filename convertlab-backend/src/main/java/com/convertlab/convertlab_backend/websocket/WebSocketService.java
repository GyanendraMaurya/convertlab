package com.convertlab.convertlab_backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Central service for pushing real-time events over WebSocket.
 *
 * <h3>Routing strategies</h3>
 * <ul>
 *   <li><b>By email</b> — authenticated user; frontend subscribes to {@code /user/queue/events}
 *       after connecting with a Bearer token.</li>
 *   <li><b>By sessionId</b> — anonymous or background services; frontend subscribes to
 *       {@code /topic/session/{sessionId}}. No auth required.</li>
 *   <li><b>Broadcast</b> — all connected clients; frontend subscribes to {@code /topic/events}.</li>
 * </ul>
 *
 * <h3>Usage examples</h3>
 * <pre>
 * // Authenticated user
 * webSocketService.sendToUser(email, WebSocketEvent.of(AI_INGEST_PROGRESS, payload));
 *
 * // Anonymous session
 * webSocketService.sendToSession(sessionId, WebSocketEvent.of(AI_INGEST_PROGRESS, payload));
 *
 * // Both at once (e.g. authenticated user who also has a session)
 * webSocketService.send(email, sessionId, WebSocketEvent.of(AI_INGEST_PROGRESS, payload));
 *
 * // Broadcast
 * webSocketService.broadcast(WebSocketEvent.of(NOTIFICATION, payload));
 * </pre>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class WebSocketService {

    /** User-specific destination — frontend subscribes to /user/queue/events */
    public static final String USER_QUEUE_DESTINATION = "/queue/events";

    /** Session-specific topic — frontend subscribes to /topic/session/{sessionId} */
    public static final String SESSION_TOPIC_PREFIX = "/topic/session/";

    /** Global broadcast — frontend subscribes to /topic/events */
    public static final String BROADCAST_DESTINATION = "/topic/events";

    private final SimpMessagingTemplate messagingTemplate;

    // ── By email (authenticated) ─────────────────────────────────────────────

    /**
     * Send to an authenticated user identified by their email (JWT principal).
     * Frontend must be subscribed to {@code /user/queue/events}.
     */
    public <T> void sendToUser(String email, WebSocketEvent<T> event) {
        sendToUser(email, USER_QUEUE_DESTINATION, event);
    }

    public <T> void sendToUser(String email, String destination, WebSocketEvent<T> event) {
        if (isBlank(email)) {
            log.warn("sendToUser called with blank email, skipping. type={}", event.getType());
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(email, destination, event);
            log.debug("WS → user [{}] @ {} | type={}", email, destination, event.getType());
        } catch (Exception e) {
            log.error("Failed to send WS event to user [{}] type={}: {}", email, event.getType(), e.getMessage(), e);
        }
    }

    // ── By sessionId (anonymous or background) ───────────────────────────────

    /**
     * Send to a browser tab / anonymous session identified by a sessionId.
     * Frontend must be subscribed to {@code /topic/session/{sessionId}}.
     *
     * This works without any JWT — useful for:
     * - Background jobs not tied to a logged-in user
     * - Pre-auth flows (e.g. progress before login completes)
     * - Per-tab isolation when the same user has multiple tabs open
     */
    public <T> void sendToSession(String sessionId, WebSocketEvent<T> event) {
        if (isBlank(sessionId)) {
            log.warn("sendToSession called with blank sessionId, skipping. type={}", event.getType());
            return;
        }
        String destination = SESSION_TOPIC_PREFIX + sessionId;
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("WS → session [{}] @ {} | type={}", sessionId, destination, event.getType());
        } catch (Exception e) {
            log.error("Failed to send WS event to session [{}] type={}: {}", sessionId, event.getType(), e.getMessage(), e);
        }
    }

    // ── Combined (authenticated user with a known sessionId) ─────────────────

    /**
     * Send to both an email-based user destination AND a session topic.
     * Use this when you have both — avoids the caller having to call twice.
     *
     * Either parameter can be null/blank — only the non-blank ones are used.
     */
    public <T> void send(String email, String sessionId, WebSocketEvent<T> event) {
        if (!isBlank(email)) {
            sendToUser(email, event);
        }
        if (!isBlank(sessionId)) {
            sendToSession(sessionId, event);
        }
        if (isBlank(email) && isBlank(sessionId)) {
            log.warn("send() called with both email and sessionId blank. type={}", event.getType());
        }
    }

    // ── Broadcast ────────────────────────────────────────────────────────────

    /**
     * Broadcast to all connected subscribers on the default destination.
     */
    public <T> void broadcast(WebSocketEvent<T> event) {
        broadcast(BROADCAST_DESTINATION, event);
    }

    public <T> void broadcast(String destination, WebSocketEvent<T> event) {
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("WS broadcast @ {} | type={}", destination, event.getType());
        } catch (Exception e) {
            log.error("Failed to broadcast WS event type={}: {}", event.getType(), e.getMessage(), e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
