package com.convertlab.convertlab_backend.security_util;

import com.convertlab.convertlab_backend.api.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Log4j2
@Component
public class JwtUtil {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    // Access token: 5 minutes
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 5 * 60;

    // Refresh token: 7 days
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    public JwtUtil(
            @Value("${jwt.access-secret}") String accessSecret,
            @Value("${jwt.refresh-secret}") String refreshSecret
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ─── Access Token ────────────────────────────────────────────────────────────

    public String generateAccessToken(String email) {
        return generateAccessToken(email, UserRole.USER);
    }

    public String generateAccessToken(String email, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("type", "access")
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ACCESS_TOKEN_EXPIRY_SECONDS)))
                .signWith(accessKey)
                .compact();
    }

    /**
     * Validates and returns the email (subject) from the access token.
     * Throws JwtException subtypes on failure.
     */
    public String validateAccessTokenAndGetEmail(String token) {
        return validateAccessTokenAndGetClaims(token).getSubject();
    }

    public Claims validateAccessTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserRole getRoleFromAccessClaims(Claims claims) {
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            return UserRole.USER;
        }

        return UserRole.valueOf(role);
    }

    public boolean isAccessTokenValid(String token) {
        try {
            validateAccessTokenAndGetEmail(token);
            return true;
        } catch (JwtException e) {
            log.debug("Access token invalid: {}", e.getMessage());
            return false;
        }
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────────

    /**
     * @param tokenId  a UUID saved to the database; lets you invalidate by ID
     */
    public String generateRefreshToken(String email, UUID tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .id(tokenId.toString())          // "jti" claim — the DB primary key
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS)))
                .signWith(refreshKey)
                .compact();
    }

    /**
     * Validates the refresh token and returns its claims.
     * Throws JwtException subtypes on failure.
     */
    public Claims validateRefreshTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getRefreshTokenExpirySeconds() {
        return REFRESH_TOKEN_EXPIRY_SECONDS;
    }

    public long getAccessTokenExpirySeconds() {
        return ACCESS_TOKEN_EXPIRY_SECONDS;
    }
}
