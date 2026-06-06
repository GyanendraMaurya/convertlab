package com.convertlab.convertlab_backend.service_resume.impl;

import com.convertlab.convertlab_backend.service_resume.HtmlToPdfConverter;
import com.convertlab.convertlab_backend.service_resume.HtmlToPdfOptions;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Log4j2
@Service
public class OpenHtmlToPdfConverter implements HtmlToPdfConverter {

    @Override
    public byte[] convert(String html, HtmlToPdfOptions options) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, options == null ? null : options.baseUri());
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            String documentName = options == null ? "resume" : options.documentName();
            log.error("Failed to convert HTML to PDF for document: {}", documentName, e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}
