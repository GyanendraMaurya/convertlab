package com.convertlab.convertlab_backend.ratelimit;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String ip, RateLimitType type) {
        String key = getBucketKey(ip, type);

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> {
            if (type == RateLimitType.UPLOAD) {
                return new TokenBucket(30, 30);
            }
            if (type == RateLimitType.SIGNUP) {
                return new TokenBucket(3, 3);
            }
            return new TokenBucket(10, 10);
        });

        return bucket.tryConsume();
    }

    private static @NonNull String getBucketKey(String ip, RateLimitType type) {
        return ip + ":" + type.name();
    }


    public TokenBucket getTokenBucket(String ip, RateLimitType type) {
        return buckets.get(getBucketKey(ip, type));
    }
}

