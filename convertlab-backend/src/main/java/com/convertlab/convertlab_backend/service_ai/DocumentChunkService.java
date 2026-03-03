package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.repository.DocumentChunkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;

    @Transactional
    public List<DocumentChunk> saveAllChunks(String fileId, List<String> chunks) {
        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("chunk " + i + ": " + chunks.get(i));
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(fileId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setCreatedAt(Instant.now());
            documentChunks.add(chunk);
        }
        return documentChunkRepository.saveAll(documentChunks);
    }
}
