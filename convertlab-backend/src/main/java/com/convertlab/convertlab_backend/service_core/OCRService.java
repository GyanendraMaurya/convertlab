package com.convertlab.convertlab_backend.service_core;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class OCRService {

    private final ITesseract tesseract;

    public OCRService() {

        this.tesseract = new Tesseract();

        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
        tesseract.setLanguage("eng");
    }

    public String extractTextFromPage(PDDocument document, int pageIndex) {

        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);
            return tesseract.doOCR(image);

        } catch (Exception e) {
            throw new RuntimeException("OCR failed for page " + pageIndex, e);
        }
    }
}