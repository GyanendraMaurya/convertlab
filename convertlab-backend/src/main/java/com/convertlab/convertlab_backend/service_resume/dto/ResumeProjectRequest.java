package com.convertlab.convertlab_backend.service_resume.dto;

import java.util.List;

public record ResumeProjectRequest(
        String name,
        String description,
        String url,
        List<String> points
) {
}
