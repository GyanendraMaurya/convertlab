package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.FileCleanerStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class LocalFileCleaner implements FileCleanerStrategy {

    @Value("${temp.folder:temp-files}")
    private String tempFolder;

    @Value("${temp.ttl-ms:3600000}") // default 1 hour
    private long ttlMs;

    @Override
    public void cleanupExpiredFiles() {
        long now = System.currentTimeMillis();
        Path root = Paths.get(tempFolder);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return;
        }

        deleteExpiredFiles(root, now);

//        Doesn't fit in current implementation as directories are required
//        deleteEmptyDirectories(root);
    }

    /**
     * Deletes files older than TTL
     */
    private void deleteExpiredFiles(Path root, long now) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            long lastModified =
                                    Files.getLastModifiedTime(file).toMillis();
                            long age = now - lastModified;

                            if (age > ttlMs) {
                                Files.deleteIfExists(file);
                                System.out.println("Deleted temp file: " + file);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to delete file: " + file);
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes empty directories (bottom-up)
     */
    private void deleteEmptyDirectories(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths
                    .filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder()) // deepest first
                    .forEach(dir -> {
                        if (dir.equals(root)) return; // keep root folder

                        try (Stream<Path> children = Files.list(dir)) {
                            if (!children.findAny().isPresent()) {
                                Files.delete(dir);
                                System.out.println("Deleted empty directory: " + dir);
                            }
                        } catch (Exception ignored) {
                            // Directory not empty or in use
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
