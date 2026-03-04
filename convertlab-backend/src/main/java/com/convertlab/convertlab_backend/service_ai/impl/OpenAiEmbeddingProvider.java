package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.exception.AiException;
import com.convertlab.convertlab_backend.service_ai.EmbeddingProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final String MODEL = "text-embedding-3-small";
    private static final int DIMENSION = 1536;
    private final WebClient openAiWebClient;

    public OpenAiEmbeddingProvider(@Qualifier("openAiWebClient") WebClient webClient) {
        this.openAiWebClient = webClient;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new AiException("Text to embed cannot be null or blank", "INVALID_EMBED_INPUT");
        }

        log.debug("Generating single embedding for text of length: {}", text.length());

        try {
            List<float[]> result = embedBatch(List.of(text));
            if (result == null || result.isEmpty()) {
                throw new AiException("No embedding returned for single text input", "EMPTY_EMBED_RESULT");
            }
            return result.getFirst();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate single embedding", e);
            throw new AiException("Failed to embed text: " + e.getMessage(), "EMBED_FAILED", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new AiException("Texts list for batch embedding cannot be null or empty", "INVALID_BATCH_INPUT");
        }

        log.debug("Generating batch embeddings for {} texts", texts.size());

        try {
            Map<String, Object> request = Map.of(
                    "model", MODEL,
                    "input", texts
            );

            Map<?, ?> response = openAiWebClient.post()
                    .uri("embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new AiException("OpenAI embeddings API returned null response", "NULL_RESPONSE");
            }

            Object dataObj = response.get("data");
            if (!(dataObj instanceof List<?> rawData)) {
                throw new AiException("OpenAI embeddings response missing 'data' field", "INVALID_RESPONSE_FORMAT");
            }

            List<float[]> result = new ArrayList<>();

            for (int i = 0; i < rawData.size(); i++) {
                Object item = rawData.get(i);
                if (!(item instanceof Map<?, ?> itemMap)) {
                    log.warn("Skipping non-map item at index {} in embeddings response", i);
                    continue;
                }

                Object embeddingObj = itemMap.get("embedding");
                if (!(embeddingObj instanceof List<?> embeddingList)) {
                    log.warn("Missing or invalid 'embedding' field at index {}", i);
                    continue;
                }

                try {
                    float[] vector = new float[embeddingList.size()];
                    for (int j = 0; j < embeddingList.size(); j++) {
                        Object val = embeddingList.get(j);
                        if (val instanceof Number num) {
                            vector[j] = num.floatValue();
                        } else {
                            throw new AiException(
                                    "Non-numeric value in embedding at index [" + i + "][" + j + "]",
                                    "INVALID_EMBEDDING_VALUE"
                            );
                        }
                    }
                    result.add(vector);
                } catch (AiException e) {
                    throw e;
                } catch (Exception e) {
                    log.error("Failed to parse embedding at index {}", i, e);
                    throw new AiException(
                            "Failed to parse embedding at index " + i + ": " + e.getMessage(),
                            "EMBEDDING_PARSE_FAILED",
                            e
                    );
                }
            }

            if (result.isEmpty()) {
                throw new AiException("No valid embeddings parsed from OpenAI response", "NO_VALID_EMBEDDINGS");
            }

            log.debug("Batch embedding complete, received {} vectors", result.size());
            return result;

        } catch (AiException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("OpenAI embeddings API returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiException(
                    "OpenAI embeddings API error: " + e.getStatusCode(),
                    "OPENAI_API_ERROR",
                    e
            );
        } catch (WebClientException e) {
            log.error("Network error while calling OpenAI embeddings API", e);
            throw new AiException(
                    "Network error while calling OpenAI embeddings: " + e.getMessage(),
                    "OPENAI_NETWORK_ERROR",
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error during batch embedding generation", e);
            throw new AiException(
                    "Failed to get embeddings from OpenAI: " + e.getMessage(),
                    "EMBEDDING_UNEXPECTED_ERROR",
                    e
            );
        }
    }

    @Override
    public String modelName() {
        return MODEL;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }
}