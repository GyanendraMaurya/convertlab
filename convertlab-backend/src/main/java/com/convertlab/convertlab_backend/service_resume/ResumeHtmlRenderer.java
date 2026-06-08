package com.convertlab.convertlab_backend.service_resume;

import com.convertlab.convertlab_backend.service_resume.dto.ResumeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ResumeHtmlRenderer {

    private final TemplateEngine templateEngine;
    private final ResumeTemplateService resumeTemplateService;
    private final ResumeRequestValidator resumeRequestValidator;

    public String render(ResumeRequest resume, String templateId) {
        return render(resume, templateId, false);
    }

    public String render(ResumeRequest resume, String templateId, boolean showPhotoPlaceholder) {
        ResumeTemplate template = resumeTemplateService.requireTemplate(templateId);
        resumeRequestValidator.validate(resume);

        Context context = new Context();
        context.setVariable("resume", resume);
        context.setVariable("showPhotoPlaceholder", showPhotoPlaceholder);

        return templateEngine.process("resume/" + template.templatePath(), context);
    }
}
