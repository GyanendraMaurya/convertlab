package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(String documentId);

    Optional<DocumentChunk> findByDocumentIdAndChunkIndex(String documentId, Integer chunkIndex);

    void deleteByDocumentId(String documentId);
}
