package com.convertlab.convertlab_backend.service_resume.dto;

import java.util.List;

public record ResumeCustomSectionItemRequest(
        String title,
        String subtitle,
        List<String> points
) {
}
