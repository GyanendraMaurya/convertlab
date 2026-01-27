package com.convertlab.convertlab_backend.ratelimit;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String ip, RateLimitType type) {
        String key = ip + ":" + type.name();

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> {
            if (type == RateLimitType.UPLOAD) {
                return new TokenBucket(40, 40); // 40 per minute
            }
            return new TokenBucket(20, 20); // 20 per minute
        });

        return bucket.tryConsume();
    }
}

