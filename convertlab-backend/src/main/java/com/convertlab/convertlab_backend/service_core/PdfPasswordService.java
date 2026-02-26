package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.api.enums.PdfPasswordAction;
import com.convertlab.convertlab_backend.exception.FileValidationException;
import com.convertlab.convertlab_backend.security_util.PdfPasswordUtils;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Log4j2
@Service
@RequiredArgsConstructor
public class PdfPasswordService {

    private final StorageService storageService;

    /**
     * Add or remove a password from a PDF based on the requested action.
     *
     * @param fileId   The stored file ID
     * @param password The password to add or the current password to remove
     * @param action   ADD or REMOVE
     * @return ExtractedFile with the resulting PDF bytes and filename
     * @throws IOException              If a file I/O error occurs
     * @throws IllegalArgumentException If the password is incorrect (REMOVE only)
     */
    public ExtractedFile processPassword(String fileId, String password, PdfPasswordAction action)
            throws IOException {

        log.info("Processing PDF password - fileId: {}, action: {}", fileId, action);

        validatePasswordInput(password, action);

        File pdfFile = storageService.loadPdf(fileId);
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);

        byte[] resultBytes = switch (action) {
            case ADD    -> PdfPasswordUtils.addPassword(pdfFile, password);
            case REMOVE -> PdfPasswordUtils.removePassword(pdfFile, password);
        };

        String outputFileName = buildOutputFileName(originalFileName, action);

        log.info("PDF password action {} completed - output: {}, size: {} bytes",
                action, outputFileName, resultBytes.length);

        return new ExtractedFile(resultBytes, outputFileName);
    }

    private void validatePasswordInput(String password, PdfPasswordAction action) {
        if (password == null || password.isBlank()) {
            throw new FileValidationException(
                    "Password cannot be empty.",
                    "INVALID_PASSWORD"
            );
        }

        if (action == PdfPasswordAction.ADD && password.length() < 4) {
            throw new FileValidationException(
                    "Password must be at least 4 characters.",
                    "PASSWORD_TOO_SHORT"
            );
        }
    }

    private String buildOutputFileName(String originalFileName, PdfPasswordAction action) {
        String nameWithoutExt = originalFileName.replaceFirst("[.][^.]+$", "");
        String suffix = action == PdfPasswordAction.ADD ? "_protected" : "_unlocked";
        return nameWithoutExt + suffix + ".pdf";
    }
}
