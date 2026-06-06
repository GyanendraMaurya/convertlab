package com.convertlab.convertlab_backend.service_resume.dto;

import java.util.List;

public record ResumeCustomSectionRequest(
        String title,
        String placement,
        List<ResumeCustomSectionItemRequest> items
) {
}
