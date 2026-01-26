package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.service_core.ImageCompressionService;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_web.controllers.dto.CompressImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageCompressionService imageCompressionService;

    @PostMapping("/compress")
    public ResponseEntity<Resource> compressImage(@RequestBody CompressImageRequest request) throws Exception {
        log.info("Compress image request received for {} file(s) with level: {}",
                request.getFileIds().size(), request.getCompressionLevel());

        try {
            // Single file - return compressed image directly
            if (request.getFileIds().size() == 1) {
                ExtractedFile compressedFile = imageCompressionService.compressSingleImage(
                        request.getFileIds().getFirst(),
                        request.getCompressionLevel()
                );

                ByteArrayResource resource = new ByteArrayResource(compressedFile.getFileBytes());

                log.info("Single image compressed successfully, output size: {} bytes",
                        compressedFile.getFileBytes().length);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + compressedFile.getFileName() + "\"")
                        .contentType(MediaType.IMAGE_JPEG)
                        .contentLength(compressedFile.getFileBytes().length)
                        .body(resource);
            }

            // Multiple files - return ZIP
            ExtractedFile zipFile = imageCompressionService.compressMultipleImages(
                    request.getFileIds(),
                    request.getCompressionLevel()
            );

            ByteArrayResource resource = new ByteArrayResource(zipFile.getFileBytes());

            log.info("Multiple images compressed successfully, ZIP size: {} bytes",
                    zipFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.getFileBytes().length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error compressing images for fileIds: {}", request.getFileIds(), e);
            throw e;
        }
    }
}
