package com.convertlab.convertlab_backend.service_storage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FileCleanupScheduler {

    private final FileCleanerStrategy cleanerStrategy;

    public FileCleanupScheduler(FileCleanerStrategy cleanerStrategy) {
        this.cleanerStrategy = cleanerStrategy;
    }

    @Scheduled(fixedRateString = "${temp.cleanup-interval-ms:1800000}") // 30 mins default
    public void performCleanup() {
        cleanerStrategy.cleanupExpiredFiles();
    }
}