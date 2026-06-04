package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.config.ValidationConfig;
import com.convertlab.convertlab_backend.service_core.ImageService;
import com.convertlab.convertlab_backend.service_core.PdfService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadLimitsResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final PdfService pdfService;
    private final ImageService imageService;
    private final ValidationConfig validationConfig;

    @PostMapping("/pdf")
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal String principal
    ) throws Exception {
        log.info("Upload request received for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadResponse response = pdfService.uploadPdf(file, principal != null);
            log.info("File uploaded successfully: {}, assetId: {}",
                    file.getOriginalFilename(), response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal String principal
    ) throws Exception {
        log.info("Image upload request received for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadResponse response = imageService.uploadImage(file, principal != null);
            log.info("Image uploaded successfully: {}, assetId: {}",
                    file.getOriginalFilename(), response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error uploading image: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<UploadLimitsResponse>> getUploadLimits(
            @AuthenticationPrincipal String principal
    ) {
        UploadLimitsResponse response = UploadLimitsResponse.builder()
                .authenticated(principal != null)
                .pdf(UploadLimitsResponse.PdfUploadLimits.builder()
                        .guestMaxSizeBytes(validationConfig.getPdfMaxSizeBytes())
                        .authenticatedMaxSizeBytes(validationConfig.getPdfAuthenticatedMaxSizeBytes())
                        .maxPages(validationConfig.getPdf().getMaxPages())
                        .allowedExtensions(validationConfig.getPdf().getAllowedExtensions())
                        .build())
                .image(UploadLimitsResponse.ImageUploadLimits.builder()
                        .guestMaxSizeBytes(validationConfig.getImageMaxSizeBytes())
                        .authenticatedMaxSizeBytes(validationConfig.getImageAuthenticatedMaxSizeBytes())
                        .maxDimension(validationConfig.getImage().getMaxDimensionPx())
                        .allowedExtensions(validationConfig.getImage().getAllowedExtensions())
                        .build())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
