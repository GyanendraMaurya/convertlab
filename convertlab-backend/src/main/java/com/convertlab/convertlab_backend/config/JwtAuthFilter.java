package com.convertlab.convertlab_backend.config;

import com.convertlab.convertlab_backend.api.enums.UserRole;
import com.convertlab.convertlab_backend.security_util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the Bearer access token from Authorization header, validates it,
 * and populates the Spring SecurityContext.
 * <p>
 * This runs AFTER the rate-limiting filter (see SecurityConfig ordering).
 * Requests without a token are still allowed through — endpoint-level
 * access control is enforced separately in SecurityConfig.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtUtil.validateAccessTokenAndGetClaims(token);
                String email = claims.getSubject();
                UserRole role = jwtUtil.getRoleFromAccessClaims(claims);

                var auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Invalid token — don't set authentication, let downstream decide
                log.debug("JWT validation failed for request [{}]: {}", request.getRequestURI(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
