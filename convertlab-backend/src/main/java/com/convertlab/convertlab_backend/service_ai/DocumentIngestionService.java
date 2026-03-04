package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.entity.Embedding1536;
import com.convertlab.convertlab_backend.repository.DocumentChunkRepository;
import com.convertlab.convertlab_backend.repository.Embedding1536Repository;
import com.convertlab.convertlab_backend.service_ai.impl.OpenAiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentChunker documentChunker;
    private final EmbeddingGenerationService embeddingGenerationService;
    private final DocumentProcessingService documentProcessingService;
    private final DocumentTextProcessor documentTextProcessor;
    private final Embedding1536Repository embeddingRepository;
    private final DocumentChunkService documentChunkService;
    private final OpenAiChatService openAiChatService;


    public void ingestDocument(String fileId) {
        String rawText = documentProcessingService.extractText(fileId);
        String cleanedText = documentTextProcessor.process(rawText);
        List<String> chunks = documentChunker.chunk(cleanedText);
        List<DocumentChunk> documentChunks = documentChunkService.saveAllChunks(fileId, chunks);
        embeddingGenerationService.generateAndStore(documentChunks);
    }

    public String queryDocument(String fileId, String query) {
        float[] queryEmbedding = embeddingGenerationService.generateQueryEmbedding(query);
        System.out.println("Generated query embedding of dimension: " + queryEmbedding.length);
        List<Embedding1536> similarEmbeddings = embeddingRepository.findSimilar(Arrays.toString(queryEmbedding), 5);
        System.out.println(similarEmbeddings);
        System.out.println(similarEmbeddings.getFirst().getChunk().getContent());

        String context = similarEmbeddings.stream()
                .map(e -> e.getChunk().getContent()) // adjust if needed
                .reduce("", (a, b) -> a + "\n\n" + b);

        // 4️⃣ Build final user prompt
        String finalPrompt = """
                Use the provided context to answer the question.
                If the answer is not in the context, say you don't know.

                Context:
                %s

                Question:
                %s
                """.formatted(context, query);

        // 5️⃣ Call LLM
        return openAiChatService.askLLM(
                "You are a helpful AI assistant.",
                finalPrompt
        );
    }
}
