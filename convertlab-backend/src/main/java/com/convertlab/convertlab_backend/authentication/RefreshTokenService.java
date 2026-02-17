package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.entity.RefreshToken;
import com.convertlab.convertlab_backend.exception.LoginException;
import com.convertlab.convertlab_backend.repository.RefreshTokenRepository;
import com.convertlab.convertlab_backend.security_util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /**
     * Creates a new refresh token, persists it, and returns the signed JWT string.
     */
    @Transactional
    public String createAndSave(String email) {
        UUID tokenId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(jwtUtil.getRefreshTokenExpirySeconds());

        RefreshToken entity = new RefreshToken(
                tokenId,
                email,
                false,
                expiresAt,
                Instant.now()
        );
        refreshTokenRepository.save(entity);

        return jwtUtil.generateRefreshToken(email, tokenId);
    }

    /**
     * Rotates a refresh token:
     *  1. Validates the JWT signature + expiry
     *  2. Looks up the token in the DB
     *  3. Detects reuse (revoked token presented → revoke all user tokens)
     *  4. Marks old token revoked, issues a brand-new one
     *
     * Returns a pair: [newRefreshTokenJwt, email]
     */
    @Transactional
    public RotationResult rotate(String rawRefreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.validateRefreshTokenAndGetClaims(rawRefreshToken);
        } catch (JwtException e) {
            throw new LoginException("Invalid or expired refresh token", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        UUID tokenId = UUID.fromString(claims.getId());
        String email  = claims.getSubject();

        // Check DB — if the row is gone or already revoked, someone is reusing a rotated token
        RefreshToken stored = refreshTokenRepository.findById(tokenId).orElse(null);

        if (stored == null) {
            // Token was never in DB or already cleaned up — just reject
            throw new LoginException("Refresh token not found", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }

        if (stored.isRevoked()) {
            // REUSE DETECTED — revoke every active token for this user
            log.warn("Refresh token reuse detected for email: {}. Revoking all tokens.", email);
            refreshTokenRepository.revokeAllByEmail(email);
            throw new LoginException(
                    "Refresh token already used. Please log in again.",
                    "REFRESH_TOKEN_REUSED",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Revoke the old token (rotation)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        // Issue a fresh refresh token
        String newRefreshToken = createAndSave(email);

        return new RotationResult(newRefreshToken, email);
    }

    /**
     * Revokes all active refresh tokens for the user (logout from all devices).
     */
    @Transactional
    public void revokeAll(String email) {
        int count = refreshTokenRepository.revokeAllByEmail(email);
        log.info("Revoked {} refresh token(s) for {}", count, email);
    }

    /**
     * Simple value carrier returned by rotate().
     */
    public record RotationResult(String newRefreshToken, String email) {}
}
