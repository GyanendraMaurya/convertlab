package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.service_ai.dto.ConversationMessage;

import java.util.List;

public interface ChatService {
    String askLLM(String systemPrompt, String userPrompt);

    String askLLM(String systemPrompt, List<ConversationMessage> conversationHistory, String userPrompt);
}
