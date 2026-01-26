package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.ImageCompressionService;
import com.convertlab.convertlab_backend.service_core.ImageService;
import com.convertlab.convertlab_backend.service_core.PdfService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UploadResponse>> upload(@RequestParam MultipartFile file) throws Exception {
        log.info("Upload request received for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadResponse response = pdfService.uploadPdf(file);
            log.info("File uploaded successfully: {}, assetId: {}",
                    file.getOriginalFilename(), response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadImage(@RequestParam MultipartFile file) throws Exception {
        log.info("Image upload request received for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadResponse response = imageService.uploadImage(file);
            log.info("Image uploaded successfully: {}, assetId: {}",
                    file.getOriginalFilename(), response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error uploading image: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }


}
