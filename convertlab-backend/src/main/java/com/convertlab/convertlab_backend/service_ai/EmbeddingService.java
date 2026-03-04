package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.entity.Embedding1536;
import com.convertlab.convertlab_backend.repository.Embedding1536Repository;
import com.convertlab.convertlab_backend.service_ai.exception.AiException;
import com.convertlab.convertlab_backend.service_ai.exception.DocumentIngestionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingProvider embeddingProvider;
    private final Embedding1536Repository embeddingRepository;

    @Transactional
    public void generateAndStore(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new DocumentIngestionException("Chunks list cannot be null or empty", "EMPTY_CHUNKS");
        }

        log.info("Generating embeddings for {} chunks", chunks.size());

        try {
            List<String> texts = chunks.stream()
                    .map(DocumentChunk::getContent)
                    .toList();

            List<float[]> vectors;
            try {
                vectors = embeddingProvider.embedBatch(texts);
            } catch (Exception e) {
                log.error("Embedding provider failed for batch of {} chunks", chunks.size(), e);
                throw new AiException(
                        "Failed to generate embeddings from provider: " + e.getMessage(),
                        "EMBEDDING_PROVIDER_FAILED",
                        e
                );
            }

            if (vectors == null || vectors.size() != chunks.size()) {
                throw new AiException(
                        "Embedding provider returned mismatched vector count. Expected: "
                                + chunks.size() + ", Got: " + (vectors == null ? 0 : vectors.size()),
                        "EMBEDDING_COUNT_MISMATCH"
                );
            }

            List<Embedding1536> embeddings = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                float[] vector = vectors.get(i);
                if (vector == null || vector.length == 0) {
                    log.warn("Empty vector returned for chunk index {}, skipping", i);
                    continue;
                }

                Embedding1536 e = new Embedding1536();
                e.setChunk(chunks.get(i));
                e.setEmbeddingModel(embeddingProvider.modelName());
                e.setEmbeddingDimension(embeddingProvider.dimension());
                e.setEmbedding(vector);
                e.setCreatedAt(Instant.now());
                embeddings.add(e);
            }

            if (embeddings.isEmpty()) {
                throw new AiException(
                        "No valid embeddings were generated from the provided chunks",
                        "NO_VALID_EMBEDDINGS"
                );
            }

            try {
                embeddingRepository.saveAll(embeddings);
                log.info("Stored {} embeddings successfully", embeddings.size());
            } catch (Exception e) {
                log.error("Failed to persist embeddings to database", e);
                throw new AiException(
                        "Failed to persist embeddings: " + e.getMessage(),
                        "EMBEDDING_PERSISTENCE_FAILED",
                        e
                );
            }

        } catch (AiException | DocumentIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during embedding generation and storage", e);
            throw new AiException(
                    "Unexpected error during embedding generation",
                    "EMBEDDING_UNEXPECTED_ERROR",
                    e
            );
        }
    }

    public float[] generateQueryEmbedding(String query) {
        if (query == null || query.isBlank()) {
            throw new AiException("Query cannot be null or blank", "INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        log.debug("Generating embedding for query of length: {}", query.length());

        try {
            float[] embedding = embeddingProvider.embed(query);

            if (embedding == null || embedding.length == 0) {
                throw new AiException(
                        "Embedding provider returned empty vector for query",
                        "EMPTY_QUERY_EMBEDDING"
                );
            }

            log.debug("Query embedding generated with dimension: {}", embedding.length);
            return embedding;

        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate embedding for query", e);
            throw new AiException(
                    "Failed to generate embedding for query: " + e.getMessage(),
                    "QUERY_EMBEDDING_FAILED",
                    e
            );
        }
    }
}