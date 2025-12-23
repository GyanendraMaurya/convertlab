package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.FileCleanerStrategy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Log4j2
@Component
public class LocalFileCleaner implements FileCleanerStrategy {

    @Value("${temp.folder:temp-files}")
    private String tempFolder;

    @Value("${temp.ttl-ms:3600000}") // default 1 hour
    private long ttlMs;

    @Override
    public void cleanupExpiredFiles() {
        log.info("Starting cleanup job for temp files in: {}", tempFolder);

        long now = System.currentTimeMillis();
        Path root = Paths.get(tempFolder);

        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.warn("Temp folder does not exist or is not a directory: {}", tempFolder);
            return;
        }

        int deletedCount = deleteExpiredFiles(root, now);

        log.info("Cleanup job completed. Deleted {} expired files", deletedCount);

        // Doesn't fit in current implementation as directories are required
        // deleteEmptyDirectories(root);
    }

    /**
     * Deletes files older than TTL
     * @return number of files deleted
     */
    private int deleteExpiredFiles(Path root, long now) {
        AtomicInteger deletedCount = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(root)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            long lastModified = Files.getLastModifiedTime(file).toMillis();
                            long age = now - lastModified;

                            if (age > ttlMs) {
                                Files.deleteIfExists(file);
                                deletedCount.incrementAndGet();
                                log.debug("Deleted expired temp file: {} (age: {} ms)",
                                        file.getFileName(), age);
                            }
                        } catch (Exception e) {
                            log.error("Failed to delete file: {}", file, e);
                        }
                    });
        } catch (Exception e) {
            log.error("Error during file cleanup", e);
        }

        return deletedCount.get();
    }

    /**
     * Deletes empty directories (bottom-up)
     */
    private void deleteEmptyDirectories(Path root) {
        log.debug("Starting empty directory cleanup");

        try (Stream<Path> paths = Files.walk(root)) {
            paths
                    .filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder()) // deepest first
                    .forEach(dir -> {
                        if (dir.equals(root)) return; // keep root folder

                        try (Stream<Path> children = Files.list(dir)) {
                            if (children.findAny().isEmpty()) {
                                Files.delete(dir);
                                log.debug("Deleted empty directory: {}", dir);
                            }
                        } catch (Exception ignored) {
                            // Directory not empty or in use
                        }
                    });
        } catch (Exception e) {
            log.error("Error during empty directory cleanup", e);
        }
    }
}