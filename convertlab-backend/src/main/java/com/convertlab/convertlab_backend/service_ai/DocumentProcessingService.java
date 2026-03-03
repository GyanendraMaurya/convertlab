package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.service_core.OCRService;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final StorageService storageService;
    private final OCRService ocrService;

    public String extractText(String fileId) {

        File pdfFile = storageService.loadPdf(fileId);
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            StringBuilder finalText = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (needsOCR(pageText)) {
                    String ocrText = ocrService.extractTextFromPage(document, page - 1);
                    finalText.append("\n").append(ocrText);
                } else {
                    finalText.append("\n").append(pageText);
                }
            }

            return finalText.toString();

        } catch (IOException e) {
            throw new RuntimeException("Failed to process PDF", e);
        }
    }

    private boolean needsOCR(String text) {
        return text == null || text.trim().length() < 50;
    }

}
