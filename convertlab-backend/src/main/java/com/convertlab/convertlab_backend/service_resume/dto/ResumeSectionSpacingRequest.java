package com.convertlab.convertlab_backend.service_resume.dto;

public record ResumeSectionSpacingRequest(
        Integer skills,
        Integer education,
        Integer links,
        Integer summary,
        Integer experience,
        Integer projects
) {
}
