package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.EmbeddingProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private static final String MODEL = "text-embedding-3-small";
    private static final int DIMENSION = 1536;
    private final WebClient webClient;
    @Value("${openai.api.key}")
    private String apiKey;

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).getFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts) {

        try {
            Map<String, Object> request = Map.of(
                    "model", MODEL,
                    "input", texts
            );

            Map response = webClient.post()
                    .uri("https://api.openai.com/v1/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> data =
                    (List<Map<String, Object>>) response.get("data");

            List<float[]> result = new ArrayList<>();

            for (Map<String, Object> item : data) {
                List<Double> embedding =
                        (List<Double>) item.get("embedding");

                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = embedding.get(i).floatValue();
                }

                result.add(vector);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get embeddings from OpenAI", e);
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
