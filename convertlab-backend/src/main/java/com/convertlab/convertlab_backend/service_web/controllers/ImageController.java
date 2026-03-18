package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.service_core.ImageCompressionService;
import com.convertlab.convertlab_backend.service_core.ImageCropService;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_web.controllers.dto.CompressImageRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.CropImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageCompressionService imageCompressionService;
    private final ImageCropService imageCropService;

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

    /**
     * Crop (and optionally rotate/flip) a single image.
     * <p>
     * All coordinates (x, y, width, height) must be in **natural image pixels**
     * (full resolution), measured AFTER any rotation/flip the client applied.
     * <p>
     * Request body example:
     * <pre>
     * {
     *   "fileId": "uuid_photo.jpg",
     *   "x": 120,
     *   "y": 80,
     *   "width": 640,
     *   "height": 480,
     *   "rotation": 90,
     *   "flipHorizontal": false,
     *   "flipVertical": false,
     *   "outputFormat": "JPEG",
     *   "quality": 90
     * }
     * </pre>
     */
    @PostMapping("/crop")
    public ResponseEntity<Resource> cropImage(@RequestBody CropImageRequest request) throws Exception {
        log.info("Crop image request — fileId: {}, crop: ({},{},{},{}), rotation: {}",
                request.getFileId(), request.getX(), request.getY(),
                request.getWidth(), request.getHeight(), request.getRotation());

        try {
            ExtractedFile result = imageCropService.cropAndTransform(
                    request.getFileId(),
                    request.getX(), request.getY(),
                    request.getWidth(), request.getHeight(),
                    request.getRotation(),
                    request.isFlipHorizontal(), request.isFlipVertical(),
                    request.getOutputFormat(),
                    request.getQuality() > 0 ? request.getQuality() : 90
            );

            ByteArrayResource resource = new ByteArrayResource(result.getFileBytes());
            String mediaType = (request.getOutputFormat() != null
                    && request.getOutputFormat().equalsIgnoreCase("PNG"))
                    ? "image/png" : "image/jpeg";

            log.info("Crop complete — output: {}, size: {} bytes", result.getFileName(), result.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(mediaType))
                    .contentLength(result.getFileBytes().length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error cropping image for fileId: {}", request.getFileId(), e);
            throw e;
        }
    }
}
