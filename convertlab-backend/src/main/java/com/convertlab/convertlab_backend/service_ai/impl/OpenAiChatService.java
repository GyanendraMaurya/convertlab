package com.convertlab.convertlab_backend.service_ai.impl;

import com.convertlab.convertlab_backend.service_ai.ChatService;
import com.convertlab.convertlab_backend.service_ai.dto.ConversationMessage;
import com.convertlab.convertlab_backend.service_ai.exception.AiException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class OpenAiChatService implements ChatService {

    private static final String MODEL = "gpt-4o-mini";
    private final WebClient openAiWebClient;

    public OpenAiChatService(@Qualifier("openAiWebClient") WebClient webClient) {
        this.openAiWebClient = webClient;
    }

    @Override
    public String askLLM(String systemPrompt, String userPrompt) {
        return askLLM(systemPrompt, List.of(), userPrompt);
    }

    @Override
    public String askLLM(String systemPrompt, List<ConversationMessage> conversationHistory, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new AiException("System prompt cannot be null or blank", "INVALID_SYSTEM_PROMPT");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new AiException("User prompt cannot be null or blank", "INVALID_USER_PROMPT", HttpStatus.BAD_REQUEST);
        }

        log.debug("Sending request to OpenAI chat - model: {}, history messages: {}, userPrompt length: {}",
                MODEL, conversationHistory == null ? 0 : conversationHistory.size(), userPrompt.length());

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            if (conversationHistory != null) {
                conversationHistory.forEach(message ->
                        messages.add(Map.of("role", message.role(), "content", message.content()))
                );
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> request = Map.of(
                    "model", MODEL,
                    "messages", messages,
                    "temperature", 0.2
            );

            JsonNode responseBody = openAiWebClient.post()
                    .uri("chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (responseBody == null) {
                throw new AiException("OpenAI returned null response", "NULL_RESPONSE");
            }

            JsonNode choices = responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiException("OpenAI response contained no choices", "EMPTY_CHOICES");
            }

            JsonNode messageContent = choices.get(0).path("message").path("content");
            if (messageContent.isMissingNode() || messageContent.isNull()) {
                throw new AiException("OpenAI response message content is missing", "MISSING_CONTENT");
            }

            String result = messageContent.asString();
            log.debug("OpenAI chat response received, length: {}", result.length());
            return result;

        } catch (AiException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("OpenAI API returned error status: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiException(
                    "OpenAI API error: " + e.getStatusCode() + " - " + e.getMessage(),
                    "OPENAI_API_ERROR",
                    e
            );
        } catch (WebClientException e) {
            log.error("Network error while calling OpenAI chat API", e);
            throw new AiException(
                    "Network error while calling OpenAI API: " + e.getMessage(),
                    "OPENAI_NETWORK_ERROR",
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error while calling OpenAI chat API", e);
            throw new AiException(
                    "Unexpected error calling OpenAI: " + e.getMessage(),
                    "OPENAI_UNEXPECTED_ERROR",
                    e
            );
        }
    }
}
