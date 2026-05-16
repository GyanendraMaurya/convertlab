package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.config.RequestContext;
import com.convertlab.convertlab_backend.entity.DocumentChunk;
import com.convertlab.convertlab_backend.entity.Embedding1536;
import com.convertlab.convertlab_backend.repository.Embedding1536Repository;
import com.convertlab.convertlab_backend.service_ai.DirectDocumentCacheService.DirectDocument;
import com.convertlab.convertlab_backend.service_ai.dto.IngestResponse;
import com.convertlab.convertlab_backend.service_ai.exception.DocumentIngestionException;
import com.convertlab.convertlab_backend.service_ai.impl.OpenAiChatService;
import com.convertlab.convertlab_backend.websocket.WebSocketEvent;
import com.convertlab.convertlab_backend.websocket.WebSocketEventType;
import com.convertlab.convertlab_backend.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int DIRECT_MODE_CHARACTER_LIMIT = 20_000;
    private static final String MODE_DIRECT = "DIRECT";
    private static final String MODE_RAG = "RAG";

    private final DocumentChunker documentChunker;
    private final EmbeddingService embeddingService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentTextProcessor documentTextProcessor;
    private final Embedding1536Repository embeddingRepository;
    private final DocumentChunkService documentChunkService;
    private final DirectDocumentCacheService directDocumentCacheService;
    private final OpenAiChatService openAiChatService;
    private final RequestContext requestContext;
    private final WebSocketService webSocketService;

    public IngestResponse ingest(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new DocumentIngestionException("File ID cannot be null or blank", "INVALID_FILE_ID", HttpStatus.BAD_REQUEST);
        }

        log.info("Starting document ingestion for fileId: {}", fileId);
        directDocumentCacheService.evict(fileId);

        try {
            String rawText = pdfTextExtractionService.extractText(fileId);
            if (rawText == null || rawText.isBlank()) {
                throw new DocumentIngestionException(
                        "No text could be extracted from document: " + fileId,
                        "TEXT_EXTRACTION_EMPTY"
                );
            }
            webSocketService.send(null, requestContext.getSessionId(), WebSocketEvent.of(WebSocketEventType.DOCUMENT_EXTRACTED, fileId ));
            log.debug("Extracted {} characters from fileId: {}", rawText.length(), fileId);

            String cleanedText = documentTextProcessor.process(rawText);
            if (cleanedText == null || cleanedText.isBlank()) {
                throw new DocumentIngestionException(
                        "Text processing resulted in empty content for fileId: " + fileId,
                        "TEXT_PROCESSING_EMPTY"
                );
            }

            webSocketService.send(null, requestContext.getSessionId(), WebSocketEvent.of(WebSocketEventType.DOCUMENT_CLEANED, fileId ));
            log.debug("Cleaned text length: {} for fileId: {}", cleanedText.length(), fileId);

            if (cleanedText.length() <= DIRECT_MODE_CHARACTER_LIMIT) {
                directDocumentCacheService.save(fileId, cleanedText);
                log.info("Document ingestion completed in direct mode for fileId: {}, chars: {}", fileId, cleanedText.length());
                return new IngestResponse(0, MODE_DIRECT, cleanedText.length());
            }

            List<String> chunks = documentChunker.chunk(cleanedText);
            if (chunks == null || chunks.isEmpty()) {
                throw new DocumentIngestionException(
                        "No chunks generated from document: " + fileId,
                        "CHUNKING_EMPTY"
                );
            }
            log.debug("Generated {} chunks for fileId: {}", chunks.size(), fileId);

            List<DocumentChunk> documentChunks = documentChunkService.saveAllChunks(fileId, chunks);

            webSocketService.send(null, requestContext.getSessionId(), WebSocketEvent.of(WebSocketEventType.DOCUMENT_CHUNKED, fileId ));
            log.debug("Saved {} chunks for fileId: {}", documentChunks.size(), fileId);

            embeddingService.generateAndStore(documentChunks, fileId);
            webSocketService.send(null, requestContext.getSessionId(), WebSocketEvent.of(WebSocketEventType.DOCUMENT_EMBEDDED, fileId ));

            log.info("Document ingestion completed successfully for fileId: {}", fileId);
            return new IngestResponse(chunks.size(), MODE_RAG, cleanedText.length());
        } catch (DocumentIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during document ingestion for fileId: {}", fileId, e);
            throw new DocumentIngestionException(
                    "Document ingestion failed for fileId: " + fileId,
                    "INGESTION_FAILED",
                    e
            );
        }
    }

    public String answerQuery(String fileId, String query) {
        if (fileId == null || fileId.isBlank()) {
            throw new DocumentIngestionException("File ID cannot be null or blank", "INVALID_FILE_ID", HttpStatus.BAD_REQUEST);
        }
        if (query == null || query.isBlank()) {
            throw new DocumentIngestionException("Query cannot be null or blank", "INVALID_QUERY");
        }

        log.info("Processing query for fileId: {}, query length: {}", fileId, query.length());

        try {
            return directDocumentCacheService.find(fileId)
                    .map(directDocument -> answerDirectDocumentQuery(directDocument, query))
                    .orElseGet(() -> answerRagQuery(fileId, query));
        } catch (DocumentIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during document query for fileId: {}", fileId, e);
            throw new DocumentIngestionException(
                    "Failed to process query for fileId: " + fileId,
                    "QUERY_FAILED",
                    e
            );
        }
    }

    private String answerDirectDocumentQuery(DirectDocument directDocument, String query) {
        log.debug("Using direct document context for fileId: {}, chars: {}",
                directDocument.fileId(), directDocument.characterCount());

        String finalPrompt = """
                Use the full document text below to answer the question.
                If the answer is not in the document, say you don't know.

                Document:
                %s

                Question:
                %s
                """.formatted(directDocument.cleanedText(), query);

        String response = openAiChatService.askLLM("You are a helpful AI assistant.", finalPrompt);
        log.info("Direct document query processed successfully for fileId: {}", directDocument.fileId());
        return response;
    }

    private String answerRagQuery(String fileId, String query) {
        try {
            float[] queryEmbedding = embeddingService.generateQueryEmbedding(query);
            log.debug("Generated query embedding of dimension: {}", queryEmbedding.length);

            List<Embedding1536> similarEmbeddings = embeddingRepository.findSimilar(
                    Arrays.toString(queryEmbedding), fileId, 5
            );

            if (similarEmbeddings == null || similarEmbeddings.isEmpty()) {
                log.warn("No similar embeddings found for query on fileId: {}", fileId);
                return "I couldn't find processed context for this document. Please upload and process the document again.";
            }

            log.debug("Found {} similar embeddings for fileId: {}", similarEmbeddings.size(), fileId);

            String context = similarEmbeddings.stream()
                    .map(e -> {
                        try {
                            return e.getChunk().getContent();
                        } catch (Exception ex) {
                            log.warn("Failed to retrieve chunk content for embedding id: {}", e.getId(), ex);
                            return "";
                        }
                    })
                    .filter(content -> !content.isBlank())
                    .reduce("", (a, b) -> a + "\n\n" + b);

            if (context.isBlank()) {
                log.warn("Context assembled from embeddings is empty for fileId: {}", fileId);
                return "Could not retrieve relevant context from the document.";
            }

            String finalPrompt = """
                    Use the provided context to answer the question.
                    If the answer is not in the context, say you don't know.

                    Context:
                    %s

                    Question:
                    %s
                    """.formatted(context, query);

            String response = openAiChatService.askLLM("You are a helpful AI assistant.", finalPrompt);
            log.info("Query processed successfully for fileId: {}", fileId);
            return response;
        } catch (DocumentIngestionException ex) {
            throw ex;
        }
    }
}
