package com.convertlab.convertlab_backend.service_ai;

public interface ChatService {
    String askLLM(String systemPrompt, String userPrompt);
}
