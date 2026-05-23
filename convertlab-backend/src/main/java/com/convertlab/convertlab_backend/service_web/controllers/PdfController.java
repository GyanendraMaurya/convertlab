package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.api.enums.ActionType;
import com.convertlab.convertlab_backend.api.enums.SplitType;
import com.convertlab.convertlab_backend.config.RequestContext;
import com.convertlab.convertlab_backend.exception.PdfPasswordException;
import com.convertlab.convertlab_backend.service_core.*;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.*;
import com.convertlab.convertlab_backend.websocket.WebSocketEvent;
import com.convertlab.convertlab_backend.websocket.WebSocketEventType;
import com.convertlab.convertlab_backend.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    private final PdfCompressionService pdfCompressionService;
    private final PdfPasswordService pdfPasswordService;
    private final PdfEditorService pdfEditorService;
    private final WebSocketService webSocketService;
    private final RequestContext requestContext;

    @GetMapping("/test/{pathVariable}")
    public ResponseEntity<ApiResponse<String>> test(@PathVariable String pathVariable) {
        log.info("Test endpoint called with pathVariable: {}", pathVariable);
        webSocketService.send(null, requestContext.getSessionId(), WebSocketEvent.of(WebSocketEventType.NOTIFICATION, "hi from websocket" ));
        return ResponseEntity.ok(ApiResponse.success("test, path variable: " + pathVariable));
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

    @PostMapping("/compress")
    public ResponseEntity<Resource> compress(@RequestBody CompressRequest request) throws Exception {
        log.info("Compress request received for {} file(s) with level: {}",
                request.getFileIds().size(), request.getCompressionLevel());

        try {
            // Single file - return compressed PDF directly
            if (request.getFileIds().size() == 1) {
                ExtractedFile compressedFile = pdfCompressionService.compressSinglePdf(
                        request.getFileIds().getFirst(),
                        request.getCompressionLevel()
                );

                ByteArrayResource resource = new ByteArrayResource(compressedFile.getFileBytes());

                log.info("Single PDF compressed successfully, output size: {} bytes",
                        compressedFile.getFileBytes().length);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + compressedFile.getFileName() + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(compressedFile.getFileBytes().length)
                        .body(resource);
            }

            // Multiple files - return ZIP
            ExtractedFile zipFile = pdfCompressionService.compressMultiplePdfs(
                    request.getFileIds(),
                    request.getCompressionLevel()
            );

            ByteArrayResource resource = new ByteArrayResource(zipFile.getFileBytes());

            log.info("Multiple PDFs compressed successfully, ZIP size: {} bytes",
                    zipFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipFile.getFileBytes().length)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error compressing PDFs for fileIds: {}", request.getFileIds(), e);
            throw e;
        }
    }

    /**
     * Single endpoint that handles both ADD and REMOVE password actions.
     * <pre>
     * Request body:
     * {
     *   "fileId": "uuid_filename.pdf",
     *   "password": "secret123",
     *   "action": "ADD"        // or "REMOVE"
     * }
     * </pre>
     * Returns the resulting PDF as a file download.
     */
    @PostMapping("/password")
    public ResponseEntity<Resource> managePassword(@RequestBody PdfPasswordRequest request) throws Exception {
        log.info("PDF password request - fileId: {}, action: {}", request.getFileId(), request.getAction());

        try {
            ExtractedFile resultFile = pdfPasswordService.processPassword(
                    request.getFileId(),
                    request.getPassword(),
                    request.getAction()
            );

            ByteArrayResource resource = new ByteArrayResource(resultFile.getFileBytes());

            log.info("PDF password action {} completed successfully for fileId: {}",
                    request.getAction(), request.getFileId());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resultFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resultFile.getFileBytes().length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            // Wrong password supplied during REMOVE
            log.warn("Incorrect password for fileId: {}", request.getFileId());
            throw new PdfPasswordException(e.getMessage(), "INCORRECT_PASSWORD");
        } catch (Exception e) {
            log.error("Error processing PDF password for fileId: {}", request.getFileId(), e);
            throw e;
        }
    }

    @PostMapping("/edit")
    public ResponseEntity<Resource> editPdf(@RequestBody PdfEditRequest request) throws Exception {
        log.info("PDF edit request received for fileId: {}, operations: {}",
                request == null ? null : request.getFileId(),
                request == null || request.getOperations() == null ? 0 : request.getOperations().size());

        try {
            ExtractedFile editedFile = pdfEditorService.editPdf(request);
            ByteArrayResource resource = new ByteArrayResource(editedFile.getFileBytes());

            log.info("PDF edit completed successfully for fileId: {}, output size: {} bytes",
                    request.getFileId(), editedFile.getFileBytes().length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + editedFile.getFileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(editedFile.getFileBytes().length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error editing PDF for fileId: {}", request == null ? null : request.getFileId(), e);
            throw e;
        }
    }
}
