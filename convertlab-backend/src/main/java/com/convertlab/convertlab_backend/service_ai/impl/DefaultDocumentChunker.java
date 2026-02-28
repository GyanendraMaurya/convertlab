package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.DocumentChunker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultDocumentChunker implements DocumentChunker {

    private static final int MAX_CHARS = 2000;     // approx 500 tokens
    private static final int OVERLAP_CHARS = 300;  // overlap window

    @Override
    public List<String> chunk(String cleanedText) {

        List<String> chunks = new ArrayList<>();

        if (cleanedText == null || cleanedText.isBlank()) {
            return chunks;
        }

        int textLength = cleanedText.length();
        int start = 0;

        while (start < textLength) {

            int end = Math.min(start + MAX_CHARS, textLength);
            String chunk = cleanedText.substring(start, end).trim();

            // Ensure no empty chunk is added
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Prevent infinite loop near the end
            if (end == textLength) {
                break;
            }

            // Move window forward with overlap
            start = end - OVERLAP_CHARS;
        }

        return chunks;
    }
}