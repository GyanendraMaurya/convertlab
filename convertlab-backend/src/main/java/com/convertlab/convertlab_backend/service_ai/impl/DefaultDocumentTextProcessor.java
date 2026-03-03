package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.DocumentTextProcessor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultDocumentTextProcessor implements DocumentTextProcessor {

    @Override
    public String process(String rawText) {

        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String text = normalizeWhitespace(rawText);
        text = removePageNumbers(text);
        text = removeRepeatedLines(text);
        text = removeCommonNoise(text);

        return text.trim();
    }

    private String normalizeWhitespace(String text) {
        text = text.replaceAll("\\r\\n", "\n");
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text;
    }

    private String removePageNumbers(String text) {
        text = text.replaceAll("(?i)page \\d+ of \\d+", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\s*$", "");
        text = text.replaceAll("(?m)^\\s*-+\\s*\\d+\\s*-+\\s*$", "");
        return text;
    }

    private String removeRepeatedLines(String text) {

        String[] lines = text.split("\n");
        Map<String, Integer> frequency = new HashMap<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                frequency.put(trimmed, frequency.getOrDefault(trimmed, 0) + 1);
            }
        }

        int threshold = Math.max(2, lines.length / 20);

        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                result.append("\n");
                continue;
            }

            if (frequency.getOrDefault(trimmed, 0) <= threshold) {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    private String removeCommonNoise(String text) {
        text = text.replaceAll("_{3,}", "");
        text = text.replaceAll("-{4,}", "");
        text = text.replaceAll("(?<=\\b\\w)\\s(?=\\w\\b)", "");
        return text;
    }
}