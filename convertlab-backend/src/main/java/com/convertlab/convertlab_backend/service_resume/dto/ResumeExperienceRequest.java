package com.convertlab.convertlab_backend.service_resume.dto;

import java.util.List;

public record ResumeExperienceRequest(
        String role,
        String company,
        String location,
        String startDate,
        String endDate,
        boolean current,
        List<String> points
) {
}
