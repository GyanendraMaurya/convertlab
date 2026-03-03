package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.entity.Embedding1536;
import com.convertlab.convertlab_backend.repository.Embedding1536Repository;
import com.pgvector.PGvector;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingGenerationService {

    private final EmbeddingProvider embeddingProvider;
    private final Embedding1536Repository embeddingRepository;

    @Transactional
    public void generateAndStore(List<DocumentChunk> chunks) {

        List<String> texts = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();

        List<float[]> vectors = embeddingProvider.embedBatch(texts);

        List<Embedding1536> embeddings = new ArrayList<>();

        for (int i = 0; i < 1; i++) {
//        for (int i = 0; i < chunks.size(); i++) {
            Embedding1536 e = new Embedding1536();
            e.setChunk(chunks.get(i));
            e.setEmbeddingModel(embeddingProvider.modelName());
            e.setEmbeddingDimension(embeddingProvider.dimension());
            e.setEmbedding(toPgVector(vectors.get(i)));
            e.setCreatedAt(Instant.now());

            embeddings.add(e);
        }

        embeddingRepository.saveAll(embeddings);
    }

    public static String toPgVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
