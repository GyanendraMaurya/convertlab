package com.convertlab.convertlab_backend.service_storage;

import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class FileCleanupScheduler {

    private final FileCleanerStrategy cleanerStrategy;

    public FileCleanupScheduler(FileCleanerStrategy cleanerStrategy) {
        this.cleanerStrategy = cleanerStrategy;
    }

    @Scheduled(fixedRateString = "${temp.cleanup-interval-ms:1800000}") // 30 mins default
    public void performCleanup() {
        log.info("Scheduled cleanup task triggered");

        try {
            cleanerStrategy.cleanupExpiredFiles();
        } catch (Exception e) {
            log.error("Error during scheduled cleanup", e);
        }
    }
}