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
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final StorageService storageService;
    private final OCRService ocrService;
    private final DocumentTextProcessor documentTextProcessor;
    private final DocumentChunker documentChunker;

    public String extractAndCleanText(String fileId) {
        String rawText = extractText(fileId);
        String cleanedText =  documentTextProcessor.process(rawText);
        List<String> chunks = documentChunker.chunk(cleanedText);
        for(int i=0; i < chunks.size(); i++) {
            System.out.println("chunk " + i + ": " + chunks.get(i));
        }
        return cleanedText;
    }

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
