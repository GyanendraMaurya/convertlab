package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class UploadLimitsResponse {
    private final boolean authenticated;
    private final PdfUploadLimits pdf;
    private final ImageUploadLimits image;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PdfUploadLimits {
        private final long guestMaxSizeBytes;
        private final long authenticatedMaxSizeBytes;
        private final int maxPages;
        private final List<String> allowedExtensions;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class ImageUploadLimits {
        private final long guestMaxSizeBytes;
        private final long authenticatedMaxSizeBytes;
        private final int maxDimension;
        private final List<String> allowedExtensions;
    }
}
