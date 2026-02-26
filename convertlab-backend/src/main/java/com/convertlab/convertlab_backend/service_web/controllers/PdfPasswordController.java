package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.enums.PdfPasswordAction;
import com.convertlab.convertlab_backend.exception.PdfPasswordException;
import com.convertlab.convertlab_backend.service_core.PdfPasswordService;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PdfPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfPasswordController {

    private final PdfPasswordService pdfPasswordService;

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
}
