package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.FileCleanerStrategy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
@Profile("s3")
public class S3FileCleanerStrategy implements FileCleanerStrategy {

    // Only clean up temp working prefixes — never touch thumbnails (they are permanent)
    private static final List<String> TEMP_PREFIXES = List.of("pdf/", "images/");

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${temp.ttl-ms:1800000}")
    private long ttlMs;

    public S3FileCleanerStrategy(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void cleanupExpiredFiles() {
        log.info("Starting S3 cleanup for bucket: {}", bucketName);

        Instant cutoff = Instant.now().minusMillis(ttlMs);
        int totalDeleted = 0;

        for (String prefix : TEMP_PREFIXES) {
            totalDeleted += cleanPrefix(prefix, cutoff);
        }

        log.info("S3 cleanup complete — deleted {} objects", totalDeleted);
    }

    private int cleanPrefix(String prefix, Instant cutoff) {
        int deleted = 0;
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<String> keysToDelete = new ArrayList<>();
            for (S3Object obj : response.contents()) {
                if (obj.lastModified().isBefore(cutoff)) {
                    keysToDelete.add(obj.key());
                }
            }

            for (String key : keysToDelete) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build());
                    log.debug("Deleted expired S3 object: {}", key);
                    deleted++;
                } catch (Exception e) {
                    log.error("Failed to delete S3 object during cleanup: {}", key, e);
                }
            }

            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;

        } while (continuationToken != null);

        return deleted;
    }
}