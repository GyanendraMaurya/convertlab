package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "embeddings_1536")
@Setter
@Getter
public class Embedding1536 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "chunk_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_embedding_chunk")
    )
    private DocumentChunk chunk;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding;

    @Column(name = "created_at")
    private Instant createdAt;

}
