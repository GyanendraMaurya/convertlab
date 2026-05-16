package com.convertlab.convertlab_backend.service_ai.dto;

import java.util.List;

public record QueryRequest(
        String fileId,
        String query,
        List<ConversationMessage> conversationHistory) {
}
