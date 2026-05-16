package com.convertlab.convertlab_backend.service_ai.dto;


public record IngestResponse(
        Integer chunkCount,
        String mode,
        Integer characterCount
) {
}
