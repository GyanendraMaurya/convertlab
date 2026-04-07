package com.convertlab.convertlab_backend.entity;

import com.convertlab.convertlab_backend.service_ai.FloatArrayToVectorConverter;
import com.convertlab.convertlab_backend.service_ai.VectorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    //    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    @JdbcTypeCode(SqlTypes.OTHER)
//    private PGvector embedding;
//    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    private String embedding;
//    @Column(name = "embedding", columnDefinition = "vector(1536)")
//    @JdbcTypeCode(java.sql.Types.OTHER)
//    @Convert(converter = FloatArrayToVectorConverter.class)
//    private float[] embedding;

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @Type(VectorType.class)
    private float[] embedding;

    @Column(name = "created_at")
    private Instant createdAt;

}
