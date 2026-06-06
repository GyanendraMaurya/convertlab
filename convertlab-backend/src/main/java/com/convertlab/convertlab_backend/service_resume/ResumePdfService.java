package com.convertlab.convertlab_backend.service_resume;

import com.convertlab.convertlab_backend.service_resume.dto.ResumeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumePdfService {

    private final ResumeHtmlRenderer resumeHtmlRenderer;
    private final HtmlToPdfConverter htmlToPdfConverter;

    public byte[] generatePdf(ResumeRequest resume, String templateId) {
        String html = resumeHtmlRenderer.render(resume, templateId);
        String documentName = resolveDocumentName(resume);

        return htmlToPdfConverter.convert(
                html,
                new HtmlToPdfOptions(documentName, null)
        );
    }

    private String resolveDocumentName(ResumeRequest resume) {
        if (resume == null || resume.fullName() == null || resume.fullName().isBlank()) {
            return "resume";
        }

        return resume.fullName()
                .trim()
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
    }
}
