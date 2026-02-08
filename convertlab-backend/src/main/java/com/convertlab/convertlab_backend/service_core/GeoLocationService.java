package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_core.pojos.GeoLocation;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public interface GeoLocationService {
    @Async
    CompletableFuture<GeoLocation> resolveLocationAsync(String ipAddress);
    Map<String, GeoLocation> getLocationCache();
    void clearCache();
    int getCacheSize();


}
