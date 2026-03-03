package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id")
    private String documentId;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name= "content")
    private String content;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(
            mappedBy = "chunk",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Embedding1536> embeddings;

    // getters & setters
}
