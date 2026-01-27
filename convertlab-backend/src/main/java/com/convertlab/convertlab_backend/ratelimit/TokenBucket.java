package com.convertlab.convertlab_backend.ratelimit;

import java.time.Instant;

public class TokenBucket {

    private final int capacity;
    private final double refillTokensPerMillis;

    private double tokens;
    private long lastRefillTimestamp;

    public TokenBucket(int capacity, int refillPerMinute) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillTokensPerMillis = refillPerMinute / 60000.0;
        this.lastRefillTimestamp = Instant.now().toEpochMilli();
    }

    public synchronized boolean tryConsume() {
        refill();

        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = Instant.now().toEpochMilli();
        long millisPassed = now - lastRefillTimestamp;

        if (millisPassed > 0) {
            double refill = millisPassed * refillTokensPerMillis;
            tokens = Math.min(capacity, tokens + refill);
            lastRefillTimestamp = now;
        }
    }
}

