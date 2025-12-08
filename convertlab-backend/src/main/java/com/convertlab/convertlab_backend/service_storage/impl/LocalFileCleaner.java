package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.FileCleanerStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.*;
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
        Path folder = Paths.get(tempFolder);

        if (!Files.exists(folder)) return;

        try (Stream<Path> stream = Files.list(folder)) {
            stream.forEach(path -> {
                File file = path.toFile();
                long age = now - file.lastModified();

                if (age > ttlMs) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Deleted temp file: " + file.getName());
                    }
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

