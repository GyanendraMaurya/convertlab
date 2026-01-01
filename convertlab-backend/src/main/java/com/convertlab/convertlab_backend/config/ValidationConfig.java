package com.convertlab.convertlab_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "validation")
public class ValidationConfig {

    private PdfValidation pdf = new PdfValidation();
    private ImageValidation image = new ImageValidation();

    @Data
    public static class PdfValidation {
        private int maxSizeMb = 15;
        private int minSizeKb = 1;
        private int maxPages = 1000;
        private List<String> allowedExtensions = List.of("pdf");
        private String allowedMimeType = "application/pdf";
    }

    @Data
    public static class ImageValidation {
        private int maxSizeMb = 10;
        private int minSizeKb = 1;
        private int maxDimensionPx = 10000;
        private List<String> allowedExtensions = List.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
        private List<String> allowedMimeTypes = List.of(
                "image/png", "image/jpeg", "image/jpg",
                "image/gif", "image/bmp", "image/webp"
        );
    }

    public long getPdfMaxSizeBytes() {
        return (long) pdf.getMaxSizeMb() * 1024 * 1024;
    }

    public long getPdfMinSizeBytes() {
        return (long) pdf.getMinSizeKb() * 1024;
    }

    public long getImageMaxSizeBytes() {
        return (long) image.getMaxSizeMb() * 1024 * 1024;
    }

    public long getImageMinSizeBytes() {
        return (long) image.getMinSizeKb() * 1024;
    }
}