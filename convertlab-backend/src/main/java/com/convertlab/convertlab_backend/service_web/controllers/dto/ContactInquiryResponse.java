package com.convertlab.convertlab_backend.service_web.controllers.dto;

import com.convertlab.convertlab_backend.api.enums.EmailNotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record ContactInquiryResponse(
        UUID id,
        Instant createdAt,
        EmailNotificationStatus emailNotificationStatus
) {
}
