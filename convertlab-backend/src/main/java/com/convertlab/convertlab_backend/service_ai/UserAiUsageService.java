package com.convertlab.convertlab_backend.service_ai;


import com.convertlab.convertlab_backend.entity.UserAiUsage;
import com.convertlab.convertlab_backend.repository.UserAiUsageRepository;
import com.convertlab.convertlab_backend.service_ai.config.AiRateLimitConfig;
import com.convertlab.convertlab_backend.service_ai.exception.AiRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserAiUsageService {

    private final UserAiUsageRepository usageRepository;
    private final AiRateLimitConfig rateLimitConfig;

    /**
     * Checks whether the user is within their daily ingest limit,
     * and if so atomically increments the counter.
     *
     * @throws AiRateLimitException when the daily limit is exceeded
     */
    @Transactional
    public void checkAndIncrementIngest(String email) {
        LocalDate today = LocalDate.now();
        UserAiUsage usage = getOrCreate(email, today);

        int limit = rateLimitConfig.getUserDailyIngestLimit();
        if (usage.getIngestCount() >= limit) {
            log.warn("Ingest daily limit ({}) exceeded for user: {}", limit, email);
            throw new AiRateLimitException(
                    "Daily PDF ingestion limit of " + limit + " reached. Try again tomorrow.",
                    "INGEST_DAILY_LIMIT_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        int updated = usageRepository.incrementIngest(email, today);
        if (updated == 0) {
            // Row was just created in this transaction — safe to set directly
            usage.setIngestCount(usage.getIngestCount() + 1);
            usageRepository.save(usage);
        }

        log.debug("Ingest count incremented for user: {} → {}/{}", email, usage.getIngestCount() + 1, limit);
    }

    /**
     * Checks whether the user is within their daily query limit,
     * and if so atomically increments the counter.
     *
     * @throws AiRateLimitException when the daily limit is exceeded
     */
    @Transactional
    public void checkAndIncrementQuery(String email) {
        LocalDate today = LocalDate.now();
        UserAiUsage usage = getOrCreate(email, today);

        int limit = rateLimitConfig.getUserDailyQueryLimit();
        if (usage.getQueryCount() >= limit) {
            log.warn("Query daily limit ({}) exceeded for user: {}", limit, email);
            throw new AiRateLimitException(
                    "Daily query limit of " + limit + " reached. Try again tomorrow.",
                    "QUERY_DAILY_LIMIT_EXCEEDED",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        int updated = usageRepository.incrementQuery(email, today);
        if (updated == 0) {
            usage.setQueryCount(usage.getQueryCount() + 1);
            usageRepository.save(usage);
        }

        log.debug("Query count incremented for user: {} → {}/{}", email, usage.getQueryCount() + 1, limit);
    }

    /**
     * Returns the today usage record for the user, creating one if it doesn't exist.
     */
    private UserAiUsage getOrCreate(String email, LocalDate date) {
        return usageRepository.findByEmailAndUsageDate(email, date)
                .orElseGet(() -> {
                    log.debug("Creating new AI usage record for user: {} on {}", email, date);
                    return usageRepository.save(new UserAiUsage(UUID.randomUUID(), email, date));
                });
    }
}
