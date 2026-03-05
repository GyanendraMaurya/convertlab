package com.convertlab.convertlab_backend.ratelimit;

import com.convertlab.convertlab_backend.service_ai.config.AiRateLimitConfig;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class IpRateLimiter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AiRateLimitConfig aiRateLimitConfig;

    public boolean allowRequest(String ip, RateLimitType type) {
        String key = getBucketKey(ip, type);

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> switch (type) {
            case UPLOAD     -> new TokenBucket(30, 30);
            case SIGNUP     -> new TokenBucket(3, 3);
            case AI_INGEST  -> new TokenBucket(
                    aiRateLimitConfig.getIpIngestPerMinute(),
                    aiRateLimitConfig.getIpIngestPerMinute()
            );
            case AI_QUERY   -> new TokenBucket(
                    aiRateLimitConfig.getIpQueryPerMinute(),
                    aiRateLimitConfig.getIpQueryPerMinute()
            );
            default         -> new TokenBucket(10, 10);
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