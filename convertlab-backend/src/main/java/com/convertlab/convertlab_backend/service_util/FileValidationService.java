package com.convertlab.convertlab_backend.service_util;

import com.convertlab.convertlab_backend.config.ValidationConfig;
import com.convertlab.convertlab_backend.exception.FileValidationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Log4j2
@Service
public class FileValidationService {

    private final ValidationConfig validationConfig;

    // Regex for illegal filename characters: \ / : * ? " < > |
    private static final Pattern ILLEGAL_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    public FileValidationService(ValidationConfig validationConfig) {
        this.validationConfig = validationConfig;
    }

    /**
     * Validate PDF file
     */
    public void validatePdfFile(MultipartFile file) {
        log.debug("Validating PDF file: {}", file.getOriginalFilename());

        // Common validations
        validateCommonRules(file);

        // File size validation
        long maxSize = validationConfig.getPdfMaxSizeBytes();
        long minSize = validationConfig.getPdfMinSizeBytes();
        validateFileSize(file, minSize, maxSize);

        // Extension validation
        String extension = getFileExtension(file.getOriginalFilename());
        if (!validationConfig.getPdf().getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new FileValidationException(
                    "Invalid file extension. Only PDF files are allowed.",
                    "INVALID_FILE_EXTENSION"
            );
        }

        // MIME type validation
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(validationConfig.getPdf().getAllowedMimeType())) {
            log.warn("Invalid MIME type for PDF: {}", contentType);
            throw new FileValidationException(
                    "Invalid file type. Expected application/pdf.",
                    "INVALID_MIME_TYPE"
            );
        }

        log.debug("PDF file validation passed: {}", file.getOriginalFilename());
    }

    /**
     * Validate image file
     */
    public void validateImageFile(MultipartFile file) {
        log.debug("Validating image file: {}", file.getOriginalFilename());

        // Common validations
        validateCommonRules(file);

        // File size validation
        long maxSize = validationConfig.getImageMaxSizeBytes();
        long minSize = validationConfig.getImageMinSizeBytes();
        validateFileSize(file, minSize, maxSize);

        // Extension validation
        String extension = getFileExtension(file.getOriginalFilename());
        if (!validationConfig.getImage().getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new FileValidationException(
                    String.format("Invalid file extension. Allowed: %s",
                            String.join(", ", validationConfig.getImage().getAllowedExtensions())),
                    "INVALID_FILE_EXTENSION"
            );
        }

        // MIME type validation
        String contentType = file.getContentType();
        if (contentType == null || !validationConfig.getImage().getAllowedMimeTypes().contains(contentType)) {
            log.warn("Invalid MIME type for image: {}", contentType);
            throw new FileValidationException(
                    "Invalid file type. Expected image file.",
                    "INVALID_MIME_TYPE"
            );
        }

        log.debug("Image file validation passed: {}", file.getOriginalFilename());
    }

    /**
     * Validate image dimensions
     */
    public void validateImageDimensions(MultipartFile file) throws IOException {
        log.debug("Validating image dimensions for: {}", file.getOriginalFilename());

        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new FileValidationException(
                    "Unable to read image file. File may be corrupted.",
                    "CORRUPTED_IMAGE"
            );
        }

        int maxDimension = validationConfig.getImage().getMaxDimensionPx();
        int width = image.getWidth();
        int height = image.getHeight();

        if (width > maxDimension || height > maxDimension) {
            throw new FileValidationException(
                    String.format("Image dimensions (%dx%d) exceed maximum allowed dimension of %dpx",
                            width, height, maxDimension),
                    "IMAGE_DIMENSION_EXCEEDED"
            );
        }

        log.debug("Image dimensions validation passed: {}x{}", width, height);
    }

    /**
     * Validate PDF page count
     */
    public void validatePdfPageCount(int pageCount) {
        log.debug("Validating PDF page count: {}", pageCount);

        int maxPages = validationConfig.getPdf().getMaxPages();

        if (pageCount > maxPages) {
            throw new FileValidationException(
                    String.format("PDF has too many pages (%d). Maximum allowed: %d",
                            pageCount, maxPages),
                    "PDF_PAGE_COUNT_EXCEEDED"
            );
        }

        if (pageCount <= 0) {
            throw new FileValidationException(
                    "PDF has no pages or is corrupted.",
                    "INVALID_PDF_PAGE_COUNT"
            );
        }

        log.debug("PDF page count validation passed: {}", pageCount);
    }

    /**
     * Common validation rules for all files
     */
    private void validateCommonRules(MultipartFile file) {
        // Check if file is null or empty
        if (file == null || file.isEmpty()) {
            throw new FileValidationException(
                    "No file uploaded or file is empty.",
                    "EMPTY_FILE"
            );
        }

        // Validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new FileValidationException(
                    "File name is missing.",
                    "MISSING_FILENAME"
            );
        }

        // Check for illegal characters in filename
        if (ILLEGAL_FILENAME_CHARS.matcher(filename).find()) {
            throw new FileValidationException(
                    "File name contains illegal characters: \\ / : * ? \" < > |",
                    "ILLEGAL_FILENAME_CHARS"
            );
        }

        // Check for file extension
        if (!filename.contains(".")) {
            throw new FileValidationException(
                    "File has no extension.",
                    "MISSING_FILE_EXTENSION"
            );
        }
    }

    /**
     * Validate file size
     */
    private void validateFileSize(MultipartFile file, long minSize, long maxSize) {
        long fileSize = file.getSize();

        if (fileSize < minSize) {
            throw new FileValidationException(
                    String.format("File size (%s) is too small. Minimum: %s",
                            formatFileSize(fileSize), formatFileSize(minSize)),
                    "FILE_TOO_SMALL"
            );
        }

        if (fileSize > maxSize) {
            throw new FileValidationException(
                    String.format("File size (%s) exceeds maximum allowed size of %s",
                            formatFileSize(fileSize), formatFileSize(maxSize)),
                    "FILE_TOO_LARGE"
            );
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Format file size for human-readable output
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Bytes";

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"Bytes", "KB", "MB", "GB"};

        return String.format("%.2f %s",
                bytes / Math.pow(1024, exp),
                units[exp]);
    }

    /**
     * Validate multiple files (for batch uploads)
     */
    public void validatePdfFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new FileValidationException(
                    "No files provided for validation.",
                    "NO_FILES"
            );
        }

        for (int i = 0; i < files.size(); i++) {
            try {
                validatePdfFile(files.get(i));
            } catch (FileValidationException e) {
                throw new FileValidationException(
                        String.format("File %d (%s): %s",
                                i + 1, files.get(i).getOriginalFilename(), e.getMessage()),
                        e.getCode()
                );
            }
        }
    }

    /**
     * Validate multiple image files
     */
    public void validateImageFiles(List<MultipartFile> files, boolean validateDimensions) {
        if (files == null || files.isEmpty()) {
            throw new FileValidationException(
                    "No files provided for validation.",
                    "NO_FILES"
            );
        }

        for (int i = 0; i < files.size(); i++) {
            try {
                validateImageFile(files.get(i));

                if (validateDimensions) {
                    validateImageDimensions(files.get(i));
                }
            } catch (FileValidationException e) {
                throw new FileValidationException(
                        String.format("File %d (%s): %s",
                                i + 1, files.get(i).getOriginalFilename(), e.getMessage()),
                        e.getCode()
                );
            } catch (IOException e) {
                throw new FileValidationException(
                        String.format("File %d (%s): Failed to read image file",
                                i + 1, files.get(i).getOriginalFilename()),
                        "IMAGE_READ_ERROR"
                );
            }
        }
    }
}