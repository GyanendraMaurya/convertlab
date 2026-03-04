package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.DocumentChunker;
import com.convertlab.convertlab_backend.service_ai.exception.DocumentIngestionException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class DefaultDocumentChunker implements DocumentChunker {

    private static final int MAX_CHARS = 2000;
    private static final int OVERLAP_CHARS = 300;

    @Override
    public List<String> chunk(String cleanedText) {
        if (cleanedText == null || cleanedText.isBlank()) {
            log.warn("Chunking called with null or blank text, returning empty list");
            return new ArrayList<>();
        }

        try {
            List<String> chunks = new ArrayList<>();
            int textLength = cleanedText.length();
            int start = 0;

            log.debug("Starting chunking for text of length: {}", textLength);

            while (start < textLength) {
                int end = Math.min(start + MAX_CHARS, textLength);

                String chunk;
                try {
                    chunk = cleanedText.substring(start, end).trim();
                } catch (StringIndexOutOfBoundsException e) {
                    log.error("Substring failed - start: {}, end: {}, textLength: {}", start, end, textLength, e);
                    break;
                }

                if (!chunk.isEmpty()) {
                    chunks.add(chunk);
                }

                if (end == textLength) {
                    break;
                }

                start = end - OVERLAP_CHARS;

                if (start >= textLength) {
                    break;
                }
            }

            log.debug("Chunking complete, produced {} chunks", chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("Unexpected error during text chunking", e);
            throw new DocumentIngestionException(
                    "Failed to chunk document text: " + e.getMessage(),
                    "CHUNKING_FAILED",
                    e
            );
        }
    }
}
