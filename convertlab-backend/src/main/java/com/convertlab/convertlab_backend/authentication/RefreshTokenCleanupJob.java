package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Periodically removes expired refresh tokens from the database.
 * This prevents the table from growing unboundedly.
 * Runs once per day by default (configurable via cron expression).
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs every 24 hours; adjust via app.jwt.cleanup-cron in application.yml
    @Scheduled(cron = "${app.jwt.cleanup-cron:0 0 3 * * *}") // 3:00 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired refresh token cleanup");
        int deleted = refreshTokenRepository.deleteExpired(Instant.now());
        log.info("Expired refresh token cleanup complete — deleted {} rows", deleted);
    }
}