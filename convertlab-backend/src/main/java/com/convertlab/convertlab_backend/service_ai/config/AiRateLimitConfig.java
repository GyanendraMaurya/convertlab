package com.convertlab.convertlab_backend.service_ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.rate-limit")
public class AiRateLimitConfig {

    /**
     * Per-user daily ingest (embedding) limit.
     * How many PDF ingestion requests a single authenticated user can make per day.
     */
    private int userDailyIngestLimit = 5;

    /**
     * Per-user daily query limit.
     * How many RAG queries a single authenticated user can make per day.
     */
    private int userDailyQueryLimit = 20;

    /**
     * Per-IP ingest requests allowed per minute (token bucket capacity + refill rate).
     * Reuses the existing TokenBucket infrastructure.
     */
    private int ipIngestPerMinute = 5;

    /**
     * Per-IP query requests allowed per minute.
     */
    private int ipQueryPerMinute = 15;
}