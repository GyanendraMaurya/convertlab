package com.convertlab.convertlab_backend.service_web.controllers.dto;

public record FeatureFlagResponse(
        String code,
        String title,
        boolean enabled,
        boolean exposeToFrontend
) {
}
