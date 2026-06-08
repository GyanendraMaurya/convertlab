package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_resume.ResumeHtmlRenderer;
import com.convertlab.convertlab_backend.service_resume.ResumePdfService;
import com.convertlab.convertlab_backend.service_resume.ResumeTemplateService;
import com.convertlab.convertlab_backend.service_resume.dto.ResumeRequest;
import com.convertlab.convertlab_backend.service_resume.dto.ResumeTemplateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeTemplateService resumeTemplateService;
    private final ResumeHtmlRenderer resumeHtmlRenderer;
    private final ResumePdfService resumePdfService;

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<ResumeTemplateResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(resumeTemplateService.getTemplates()));
    }

    @PostMapping(value = "/preview/{templateId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(
            @PathVariable String templateId,
            @RequestBody ResumeRequest resume
    ) {
        log.info("Resume preview requested for template: {}", templateId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resumeHtmlRenderer.render(resume, templateId, true));
    }

    @PostMapping("/download/{templateId}")
    public ResponseEntity<Resource> download(
            @PathVariable String templateId,
            @RequestBody ResumeRequest resume
    ) {
        log.info("Resume PDF download requested for template: {}", templateId);
        byte[] pdf = resumePdfService.generatePdf(resume, templateId);
        ByteArrayResource resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(resource);
    }
}
