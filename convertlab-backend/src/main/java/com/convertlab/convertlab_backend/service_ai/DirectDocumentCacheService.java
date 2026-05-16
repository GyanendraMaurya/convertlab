package com.convertlab.convertlab_backend.service_ai;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Log4j2
@Service
public class DirectDocumentCacheService {

    private final ConcurrentMap<String, DirectDocument> documents = new ConcurrentHashMap<>();

    public void save(String fileId, String cleanedText) {
        if (fileId == null || fileId.isBlank() || cleanedText == null || cleanedText.isBlank()) {
            log.warn("Skipping direct document cache save because fileId or text is blank");
            return;
        }

        documents.put(fileId, new DirectDocument(fileId, cleanedText, cleanedText.length(), Instant.now()));
        log.debug("Cached direct document context for fileId: {}, chars: {}", fileId, cleanedText.length());
    }

    public Optional<DirectDocument> find(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(documents.get(fileId));
    }

    public void evict(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }

        documents.remove(fileId);
    }

    public record DirectDocument(
            String fileId,
            String cleanedText,
            Integer characterCount,
            Instant createdAt
    ) {
    }
}
