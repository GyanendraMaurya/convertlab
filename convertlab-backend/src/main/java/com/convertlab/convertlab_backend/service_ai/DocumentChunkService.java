package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.repository.DocumentChunkRepository;
import com.convertlab.convertlab_backend.service_ai.exception.DocumentIngestionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;

    @Transactional
    public List<DocumentChunk> saveAllChunks(String fileId, List<String> chunks) {
        if (fileId == null || fileId.isBlank()) {
            throw new DocumentIngestionException("File ID cannot be null or blank", "INVALID_FILE_ID");
        }
        if (chunks == null || chunks.isEmpty()) {
            throw new DocumentIngestionException("Chunks list cannot be null or empty", "EMPTY_CHUNKS");
        }

        try {
            List<DocumentChunk> documentChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                if (chunkContent == null || chunkContent.isBlank()) {
                    log.warn("Skipping blank chunk at index {} for fileId: {}", i, fileId);
                    continue;
                }
                log.debug("Preparing chunk {}/{} for fileId: {}", i + 1, chunks.size(), fileId);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(fileId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunkContent);
                chunk.setCreatedAt(Instant.now());
                documentChunks.add(chunk);
            }

            if (documentChunks.isEmpty()) {
                throw new DocumentIngestionException(
                        "No valid chunks to save for fileId: " + fileId,
                        "NO_VALID_CHUNKS"
                );
            }

            List<DocumentChunk> saved = documentChunkRepository.saveAll(documentChunks);
            log.info("Saved {} chunks for fileId: {}", saved.size(), fileId);
            return saved;

        } catch (DocumentIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to save chunks for fileId: {}", fileId, e);
            throw new DocumentIngestionException(
                    "Failed to persist document chunks for fileId: " + fileId,
                    "CHUNK_PERSISTENCE_FAILED",
                    e
            );
        }
    }
}