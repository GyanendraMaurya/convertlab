package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.entity.Embedding1536;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface Embedding1536Repository extends JpaRepository<Embedding1536, Long> {

    List<Embedding1536> findByChunkId(Long chunkId);

    Optional<Embedding1536> findByChunkIdAndEmbeddingModel(Long chunkId, String embeddingModel);

//    @Query(value = """
//    SELECT * FROM embeddings_1536
//    ORDER BY embedding <-> CAST(:vector AS vector)
//    LIMIT :limit
//    """, nativeQuery = true)
//    List<Embedding1536> findNearest(
//            @Param("vector") String vector,
//            @Param("limit") int limit
//    );

//    @Query(value = """
//        SELECT *
//        FROM embeddings_1536
//        ORDER BY embedding <-> :embedding
//        LIMIT :limit
//        """, nativeQuery = true)
//    List<Embedding1536> findSimilar(
//            @Param("embedding") float[] embedding,
//            @Param("limit") int limit
//    );

    // Cosine similarity — best for text embeddings (OpenAI, etc.)
    @Query(value = """
            SELECT * FROM embeddings_1536
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Embedding1536> findSimilar(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );


}
