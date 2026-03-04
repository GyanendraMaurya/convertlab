package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.service_ai.exception.DocumentIngestionException;
import com.convertlab.convertlab_backend.service_core.OCRService;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Log4j2
@Service
@RequiredArgsConstructor
public class PdfTextExtractionService {

    private final StorageService storageService;
    private final OCRService ocrService;

    public String extractText(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new DocumentIngestionException("File ID cannot be null or blank", "INVALID_FILE_ID");
        }

        log.info("Starting text extraction for fileId: {}", fileId);

        File pdfFile = storageService.loadPdf(fileId);

        if (pdfFile == null || !pdfFile.exists()) {
            throw new DocumentIngestionException(
                    "PDF file not found for fileId: " + fileId,
                    "PDF_FILE_NOT_FOUND",
                    HttpStatus.BAD_REQUEST
            );
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();

            if (totalPages == 0) {
                throw new DocumentIngestionException(
                        "PDF has no pages for fileId: " + fileId,
                        "PDF_EMPTY",
                        HttpStatus.BAD_REQUEST
                );
            }

            log.debug("Processing {} pages for fileId: {}", totalPages, fileId);

            StringBuilder finalText = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= totalPages; page++) {
                try {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(document).trim();

                    if (needsOCR(pageText)) {
                        log.debug("Page {} needs OCR for fileId: {}", page, fileId);
                        try {
                            String ocrText = ocrService.extractTextFromPage(document, page - 1);
                            finalText.append("\n").append(ocrText);
                        } catch (Exception ocrEx) {
                            log.warn("OCR failed for page {} of fileId: {}, skipping page. Reason: {}",
                                    page, fileId, ocrEx.getMessage());
                        }
                    } else {
                        finalText.append("\n").append(pageText);
                    }

                } catch (IOException pageEx) {
                    log.warn("Failed to extract text from page {} of fileId: {}, skipping. Reason: {}",
                            page, fileId, pageEx.getMessage());
                }
            }

            String extractedText = finalText.toString();
            if (extractedText.isBlank()) {
                throw new DocumentIngestionException(
                        "Text extraction yielded no content for fileId: " + fileId,
                        "EXTRACTION_EMPTY"
                );
            }

            log.info("Text extraction completed for fileId: {}, total chars: {}", fileId, extractedText.length());
            return extractedText;

        } catch (DocumentIngestionException e) {
            throw e;
        } catch (IOException e) {
            log.error("IO error while processing PDF for fileId: {}", fileId, e);
            throw new DocumentIngestionException(
                    "Failed to read PDF for fileId: " + fileId,
                    "PDF_READ_FAILED",
                    e
            );
        } catch (Exception e) {
            log.error("Unexpected error during text extraction for fileId: {}", fileId, e);
            throw new DocumentIngestionException(
                    "Unexpected error during text extraction for fileId: " + fileId,
                    "EXTRACTION_UNEXPECTED_ERROR",
                    e
            );
        }
    }

    private boolean needsOCR(String text) {
        return text == null || text.trim().length() < 50;
    }
}