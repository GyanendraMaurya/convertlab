package com.convertlab.convertlab_backend.service_web.controllers.dto;

import java.time.Instant;

public record CreateBroadcastRequest(
        String message,
        Instant expiresAt
) {
}
