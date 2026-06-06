package com.convertlab.convertlab_backend.service_resume;

import com.convertlab.convertlab_backend.service_resume.dto.ResumeTemplateResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ResumeTemplateService {

    private final Map<String, ResumeTemplate> templates = Map.of(
            "classic",
            new ResumeTemplate(
                    "classic",
                    "Classic",
                    "Clean A4 resume with structured sections and print-safe spacing.",
                    "classic"
            )
    );

    public List<ResumeTemplateResponse> getTemplates() {
        return templates.values().stream()
                .map(template -> new ResumeTemplateResponse(
                        template.id(),
                        template.name(),
                        template.description()
                ))
                .toList();
    }

    public ResumeTemplate requireTemplate(String templateId) {
        ResumeTemplate template = templates.get(templateId);

        if (template == null) {
            throw new IllegalArgumentException("Unknown resume template: " + templateId);
        }

        return template;
    }
}
