package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.api.enums.CompressionLevel;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfCompressionUtils;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
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
public class PdfCompressionService {

    private final StorageService storageService;

    /**
     * Compress a single PDF file
     *
     * @param fileId           The file ID of the PDF to compress
     * @param compressionLevel The compression level
     * @return ExtractedFile containing compressed PDF
     * @throws IOException If compression fails
     */
    public ExtractedFile compressSinglePdf(String fileId, CompressionLevel compressionLevel) throws IOException {
        log.info("Compressing single PDF - fileId: {}, level: {}", fileId, compressionLevel);

        File pdfFile = storageService.loadPdf(fileId);
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);

        byte[] compressedBytes = PdfCompressionUtils.compressPdf(pdfFile, compressionLevel);

        // Generate output filename
        String outputFileName = generateCompressedFileName(originalFileName, compressionLevel);

        log.info("Single PDF compressed successfully - Output: {}, Size: {} bytes",
                outputFileName, compressedBytes.length);

        return new ExtractedFile(compressedBytes, outputFileName);
    }

    /**
     * Compress multiple PDF files and return as ZIP
     *
     * @param fileIds          List of file IDs to compress
     * @param compressionLevel The compression level
     * @return ExtractedFile containing ZIP with compressed PDFs
     * @throws IOException If compression fails
     */
    public ExtractedFile compressMultiplePdfs(List<String> fileIds, CompressionLevel compressionLevel) throws IOException {
        log.info("Compressing {} PDFs into ZIP - level: {}", fileIds.size(), compressionLevel);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {

            for (int i = 0; i < fileIds.size(); i++) {
                String fileId = fileIds.get(i);

                try {
                    log.debug("Processing PDF {}/{}: {}", i + 1, fileIds.size(), fileId);

                    File pdfFile = storageService.loadPdf(fileId);
                    String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);

                    byte[] compressedBytes = PdfCompressionUtils.compressPdf(pdfFile, compressionLevel);

                    // Generate unique filename for ZIP entry
                    String zipEntryName = generateCompressedFileName(originalFileName, compressionLevel);

                    // Add to ZIP
                    zipOut.putNextEntry(new ZipEntry(zipEntryName));
                    zipOut.write(compressedBytes);
                    zipOut.closeEntry();

                    log.debug("Added to ZIP: {} ({} bytes)", zipEntryName, compressedBytes.length);

                } catch (Exception e) {
                    log.error("Failed to compress PDF: {}", fileId, e);
                    // Continue with other files even if one fails
                }
            }

            zipOut.finish();
            byte[] zipBytes = baos.toByteArray();

            log.info("Multiple PDFs compressed successfully - ZIP size: {} bytes", zipBytes.length);

            return new ExtractedFile(zipBytes, "ConvertLab_Compressed.zip");

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

        return nameWithoutExt + levelSuffix + ".pdf";
    }
}
