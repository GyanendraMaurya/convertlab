package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.api.enums.ActionType;
import com.convertlab.convertlab_backend.api.enums.SplitType;
import com.convertlab.convertlab_backend.service_core.ImageService;
import com.convertlab.convertlab_backend.service_core.PdfService;
import com.convertlab.convertlab_backend.service_core.PdfSplitService;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Log4j2
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final StorageService storageService;
    private final PdfSplitService pdfSplitService;
    private final ImageService imageService;

    @GetMapping("/test/{pathVariable}")
    public ResponseEntity<ApiResponse<String>> test(@PathVariable String pathVariable) {
        log.info("Test endpoint called with pathVariable: {}", pathVariable);
        return ResponseEntity.ok(ApiResponse.success("test, path variable: " + pathVariable));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UploadResponse>> upload(@RequestParam MultipartFile file) throws Exception {
        log.info("Upload request received for file: {} (size: {} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            UploadResponse response = pdfService.uploadPdf(file);
            log.info("File uploaded successfully: {} with {} pages, assetId: {}",
                    file.getOriginalFilename(), response.getPageCount(), response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            throw e;
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<Resource> extract(@RequestBody ExtractRequest request) throws Exception {
        log.info("Extract request received for fileId: {}, pageRange: {}, actionType: {}",
                request.getFileId(), request.getPageRange(), request.getActionType());

        try {
            PdfUtils.validateInputRangePattern(request.getPageRange());
            File pdfFile = storageService.loadPdf(request.getFileId());
            int totalPages = PdfUtils.getPageCount(pdfFile);
            List<Integer> pagesToKeep = PdfUtils.getPageRanges(
                    request.getPageRange(),
                    totalPages,
                    request.getActionType().equals(ActionType.KEEP)
            );

            log.debug("Extracting pages: {} from total pages: {}", pagesToKeep, totalPages);

            ExtractedFile extractedFile = pdfService.extractPages(request, pagesToKeep);

            ByteArrayResource resource = new ByteArrayResource(extractedFile.getFileBytes());

            log.info("Pages extracted successfully for fileId: {}, output size: {} bytes",
                    request.getFileId(), extractedFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + "extracted_" + extractedFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(extractedFile.getFileBytes().length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error extracting pages for fileId: {}", request.getFileId(), e);
            throw e;
        }
    }

    @GetMapping("/thumbnail/{assetId}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String assetId) throws Exception {
        log.debug("Thumbnail request for assetId: {}", assetId);

        try {
            File image = storageService.loadThumbnail(assetId);
            byte[] bytes = Files.readAllBytes(image.toPath());

            log.debug("Thumbnail loaded successfully for assetId: {}, size: {} bytes",
                    assetId, bytes.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Error loading thumbnail for assetId: {}", assetId, e);
            throw e;
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<Resource> merge(@RequestBody MergeRequest request) throws Exception {
        log.info("Merge request received for {} files: {}",
                request.getFileIds().size(), request.getFileIds());

        try {
            ExtractedFile mergedFile = pdfService.mergePdfs(request);

            ByteArrayResource resource = new ByteArrayResource(mergedFile.getFileBytes());

            log.info("PDFs merged successfully, output size: {} bytes",
                    mergedFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + mergedFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(mergedFile.getFileBytes().length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error merging PDFs for fileIds: {}", request.getFileIds(), e);
            throw e;
        }
    }

    @PostMapping("/split")
    public ResponseEntity<StreamingResponseBody> split(@RequestBody SplitRequest request) throws Exception {
        log.info("Split request received for fileId: {}, pageRange: {}, splitType: {}",
                request.getFileId(), request.getPageRange(), request.getSplitType());

        try {
            // Validate input if split by range
            if (request.getSplitType() == SplitType.BY_RANGE) {
                PdfUtils.validateInputRangePattern(request.getPageRange());
            }

            // Load PDF file
            File pdfFile = storageService.loadPdf(request.getFileId());

            if (!pdfFile.exists()) {
                log.error("PDF file not found for fileId: {}", request.getFileId());
                throw new RuntimeException("PDF file not found");
            }

            String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);
            String baseFileName = originalFileName.replaceFirst("[.][^.]+$", ""); // Remove extension

            // Create streaming response
            StreamingResponseBody responseBody = outputStream -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                    pdfSplitService.splitIntoZip(
                            pdfFile,
                            request.getPageRange(),
                            request.getSplitType(),
                            zipOut
                    );
                    zipOut.finish();
                    log.info("Split PDF streaming completed for fileId: {}", request.getFileId());
                } catch (Exception e) {
                    log.error("Error during PDF split streaming for fileId: {}", request.getFileId(), e);
                    throw new RuntimeException("Error splitting PDF", e);
                }
            };

            String zipFileName = baseFileName + "_split.zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(responseBody);

        } catch (Exception e) {
            log.error("Error splitting PDF for fileId: {}", request.getFileId(), e);
            throw e;
        }


    }

    @PostMapping("/upload-image")
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

    @PostMapping("/images-to-pdf")
    public ResponseEntity<Resource> imagesToPdf(@RequestBody ImageToPdfRequest request) throws Exception {
        log.info("Image to PDF conversion request received for {} images", request.getImages().size());

        try {
            ExtractedFile pdfFile = imageService.convertImagesToPdf(request);

            ByteArrayResource resource = new ByteArrayResource(pdfFile.getFileBytes());

            log.info("Images converted to PDF successfully, output size: {} bytes",
                    pdfFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + pdfFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfFile.getFileBytes().length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error converting images to PDF", e);
            throw e;
        }
    }
}