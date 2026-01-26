package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.api.enums.CompressionLevel;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.ImageCompressionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Service
@RequiredArgsConstructor
public class ImageCompressionService {

    private final StorageService storageService;

    /**
     * Compress a single image file
     *
     * @param fileId           The file ID of the image to compress
     * @param compressionLevel The compression level
     * @return ExtractedFile containing compressed image
     * @throws IOException If compression fails
     */
    public ExtractedFile compressSingleImage(String fileId, CompressionLevel compressionLevel) throws IOException {
        log.info("Compressing single image - fileId: {}, level: {}", fileId, compressionLevel);

        File imageFile = storageService.loadImage(fileId);
        String originalFileName = getOriginalFileName(imageFile);

        byte[] compressedBytes = ImageCompressionUtils.compressImage(imageFile, compressionLevel);

        // Generate output filename
        String outputFileName = generateCompressedFileName(originalFileName, compressionLevel);

        log.info("Single image compressed successfully - Output: {}, Size: {} bytes",
                outputFileName, compressedBytes.length);

        return new ExtractedFile(compressedBytes, outputFileName);
    }

    /**
     * Compress multiple image files and return as ZIP
     *
     * @param fileIds          List of file IDs to compress
     * @param compressionLevel The compression level
     * @return ExtractedFile containing ZIP with compressed images
     * @throws IOException If compression fails
     */
    public ExtractedFile compressMultipleImages(List<String> fileIds, CompressionLevel compressionLevel) throws IOException {
        log.info("Compressing {} images into ZIP - level: {}", fileIds.size(), compressionLevel);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (int i = 0; i < fileIds.size(); i++) {
                String fileId = fileIds.get(i);

                try {
                    log.debug("Processing image {}/{}: {}", i + 1, fileIds.size(), fileId);

                    File imageFile = storageService.loadImage(fileId);
                    String originalFileName = getOriginalFileName(imageFile);

                    byte[] compressedBytes = ImageCompressionUtils.compressImage(imageFile, compressionLevel);

                    // Generate unique filename for ZIP entry
                    String zipEntryName = generateCompressedFileName(originalFileName, compressionLevel);

                    // Add to ZIP
                    zipOut.putNextEntry(new ZipEntry(zipEntryName));
                    zipOut.write(compressedBytes);
                    zipOut.closeEntry();

                    log.debug("Added to ZIP: {} ({} bytes)", zipEntryName, compressedBytes.length);

                } catch (Exception e) {
                    log.error("Failed to compress image: {}", fileId, e);
                    // Continue with other files even if one fails
                }
            }

            zipOut.finish();
            byte[] zipBytes = baos.toByteArray();

            log.info("Multiple images compressed successfully - ZIP size: {} bytes", zipBytes.length);

            return new ExtractedFile(zipBytes, "ConvertLab_Compressed_Images.zip");

        }
    }

    /**
     * Generate output filename with compression indicator
     */
    private String generateCompressedFileName(String originalFileName, CompressionLevel level) {
        // Remove extension
        String nameWithoutExt = originalFileName.replaceFirst("[.][^.]+$", "");

        // Add compression level indicator
        String levelSuffix = switch (level) {
            case LOW -> "_compressed_low";
            case MEDIUM -> "_compressed_medium";
            case HIGH -> "_compressed_high";
        };

        // Always use .jpg for compressed images
        return nameWithoutExt + levelSuffix + ".jpg";
    }

    /**
     * Get original filename from stored file
     */
    private String getOriginalFileName(File imageFile) {
        if (imageFile == null) {
            return "image.jpg";
        }

        String fileName = imageFile.getName();

        // Remove UUID prefix if present (format: UUID_originalname.ext)
        String[] parts = fileName.split("_", 2);
        if (parts.length > 1) {
            return parts[1];
        }

        return fileName;
    }
}