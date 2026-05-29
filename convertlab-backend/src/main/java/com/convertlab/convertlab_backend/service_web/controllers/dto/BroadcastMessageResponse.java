package com.convertlab.convertlab_backend.service_web.controllers.dto;

import java.time.Instant;
import java.util.UUID;

public record BroadcastMessageResponse(
        UUID id,
        String message,
        Instant createdAt,
        Instant expiresAt,
        boolean active
) {
}
