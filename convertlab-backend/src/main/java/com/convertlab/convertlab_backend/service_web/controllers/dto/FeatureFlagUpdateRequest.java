package com.convertlab.convertlab_backend.service_web.controllers.dto;

public record FeatureFlagUpdateRequest(
        String code,
        boolean enabled
) {
}
