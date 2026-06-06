package com.convertlab.convertlab_backend.service_resume.dto;

public record ResumeEducationRequest(
        String degree,
        String institution,
        String location,
        String duration,
        String details
) {
}
