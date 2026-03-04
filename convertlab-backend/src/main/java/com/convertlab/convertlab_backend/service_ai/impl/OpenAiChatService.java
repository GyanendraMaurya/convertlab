package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.ChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService implements ChatService {

    private static final String MODEL = "gpt-4o-mini";
    private final WebClient openAiWebClient;

    public OpenAiChatService(@Qualifier("openAiWebClient") WebClient webClient) {
        this.openAiWebClient = webClient;
    }

    @Override
    public String askLLM(String systemPrompt, String userPrompt) {

        Map<String, Object> request = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        return openAiWebClient.post()
                .uri("chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("choices")
                        .get(0)
                        .get("message")
                        .get("content")
                        .asString())
                .block();
    }
}
