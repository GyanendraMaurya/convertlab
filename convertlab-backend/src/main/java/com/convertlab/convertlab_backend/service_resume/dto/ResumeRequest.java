package com.convertlab.convertlab_backend.service_resume.dto;

import java.util.List;

public record ResumeRequest(
        String fullName,
        String title,
        String email,
        String phone,
        String location,
        String summary,
        String photoDataUri,
        List<String> skills,
        List<ResumeExperienceRequest> experience,
        List<ResumeEducationRequest> education,
        List<ResumeProjectRequest> projects,
        List<ResumeLinkRequest> links,
        List<ResumeCustomSectionRequest> customSections
) {
}
