package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentChunker documentChunker;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentTextProcessor documentTextProcessor;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentChunkService documentChunkService;


    public void ingestDocument(String fileId) {
        String rawText = documentProcessingService.extractText(fileId);
        String cleanedText = documentTextProcessor.process(rawText);
        List<String> chunks = documentChunker.chunk(cleanedText);
        List<DocumentChunk> documentChunks = documentChunkService.saveAllChunks(fileId, chunks);
        embeddingGenerationService.generateAndStore(documentChunks);
    }
}
