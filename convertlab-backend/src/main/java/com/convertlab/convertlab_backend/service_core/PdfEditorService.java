package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.exception.FileValidationException;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PdfEditOperation;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PdfEditRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class PdfEditorService {

    private static final float DEFAULT_FONT_SIZE = 14f;
    private static final float TEXT_PADDING = 2f;

    private final StorageService storageService;

    public ExtractedFile editPdf(PdfEditRequest request) throws IOException {
        validateRequest(request);

        File pdfFile = storageService.loadPdf(request.getFileId());
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);

        try (PDDocument document = Loader.loadPDF(pdfFile);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            for (PdfEditOperation operation : request.getOperations()) {
                applyOperation(document, operation);
            }

            document.save(output);
            return new ExtractedFile(output.toByteArray(), buildOutputFileName(originalFileName));
        }
    }

    private void validateRequest(PdfEditRequest request) {
        if (request == null || request.getFileId() == null || request.getFileId().isBlank()) {
            throw new FileValidationException("PDF file is required.", "INVALID_PDF_EDIT_REQUEST");
        }
        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new FileValidationException("Add at least one edit before exporting.", "NO_PDF_EDITS");
        }
    }

    private void applyOperation(PDDocument document, PdfEditOperation operation) throws IOException {
        int pageIndex = operation.getPageNumber() - 1;
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new FileValidationException("Invalid page number in edit request.", "INVALID_PDF_EDIT_PAGE");
        }

        PDPage page = document.getPage(pageIndex);
        PDRectangle mediaBox = page.getMediaBox();
        float pageHeight = mediaBox.getHeight();
        float x = (float) operation.getX();
        float width = Math.max(1f, (float) operation.getWidth());
        float height = Math.max(1f, (float) operation.getHeight());
        float bottomY = pageHeight - (float)operation.getY() - height;

        try (PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        )) {
            if (operation.isCoverEnabled()) {
                Color coverColor = parseColor(operation.getCoverColor(), Color.WHITE);
                contentStream.setNonStrokingColor(coverColor);
                contentStream.addRect(x, bottomY, width, height);
                contentStream.fill();
            }

            if (operation.getText() != null && !operation.getText().isBlank()) {
                writeText(contentStream, operation, x, bottomY, width, height);
            }
        }
    }

    private void writeText(
            PDPageContentStream contentStream,
            PdfEditOperation operation,
            float x,
            float bottomY,
            float width,
            float height
    ) throws IOException {
        PDFont font = resolveFont(operation);
        float fontSize = operation.getFontSize() > 0 ? (float) operation.getFontSize() : DEFAULT_FONT_SIZE;
        Color textColor = parseColor(operation.getTextColor(), Color.BLACK);
        String[] lines = operation.getText().replace("\r", "").split("\n");
        float lineHeight = fontSize * 1.2f;
        float currentY = bottomY + height - fontSize - TEXT_PADDING;

        contentStream.setNonStrokingColor(textColor);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);

        for (String line : lines) {
            if (currentY < bottomY) {
                break;
            }

            String safeLine = sanitizePdfText(line);
            float textWidth = font.getStringWidth(safeLine) / 1000f * fontSize;
            float lineX = switch (normalizeAlignment(operation.getAlignment())) {
                case "center" -> x + Math.max(0f, (width - textWidth) / 2f);
                case "right" -> x + Math.max(0f, width - textWidth - TEXT_PADDING);
                default -> x + TEXT_PADDING;
            };

            contentStream.newLineAtOffset(lineX, currentY);
            contentStream.showText(safeLine);
            contentStream.newLineAtOffset(-lineX, -currentY);
            currentY -= lineHeight;
        }

        contentStream.endText();
    }

    private PDFont resolveFont(PdfEditOperation operation) {
        String family = operation.getFontFamily() == null ? "" : operation.getFontFamily().toLowerCase();
        boolean serif = family.contains("times") || family.contains("serif");
        boolean monospace = family.contains("courier") || family.contains("mono");

        Standard14Fonts.FontName fontName;
        if (monospace) {
            if (operation.isBold() && operation.isItalic()) {
                fontName = Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE;
            } else if (operation.isBold()) {
                fontName = Standard14Fonts.FontName.COURIER_BOLD;
            } else if (operation.isItalic()) {
                fontName = Standard14Fonts.FontName.COURIER_OBLIQUE;
            } else {
                fontName = Standard14Fonts.FontName.COURIER;
            }
        } else if (serif) {
            if (operation.isBold() && operation.isItalic()) {
                fontName = Standard14Fonts.FontName.TIMES_BOLD_ITALIC;
            } else if (operation.isBold()) {
                fontName = Standard14Fonts.FontName.TIMES_BOLD;
            } else if (operation.isItalic()) {
                fontName = Standard14Fonts.FontName.TIMES_ITALIC;
            } else {
                fontName = Standard14Fonts.FontName.TIMES_ROMAN;
            }
        } else {
            if (operation.isBold() && operation.isItalic()) {
                fontName = Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
            } else if (operation.isBold()) {
                fontName = Standard14Fonts.FontName.HELVETICA_BOLD;
            } else if (operation.isItalic()) {
                fontName = Standard14Fonts.FontName.HELVETICA_OBLIQUE;
            } else {
                fontName = Standard14Fonts.FontName.HELVETICA;
            }
        }

        return new PDType1Font(fontName);
    }

    private Color parseColor(String hex, Color fallback) {
        if (hex == null || !hex.matches("^#[0-9a-fA-F]{6}$")) {
            return fallback;
        }
        return new Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16)
        );
    }

    private String sanitizePdfText(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder safeText = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character == '\t') {
                safeText.append(' ');
            } else if (character >= 32 && character <= 255) {
                safeText.append(character);
            } else if (!Character.isISOControl(character)) {
                safeText.append('?');
            }
        }
        return safeText.toString();
    }

    private String normalizeAlignment(String alignment) {
        if (List.of("left", "center", "right").contains(alignment)) {
            return alignment;
        }
        return "left";
    }

    private String buildOutputFileName(String originalFileName) {
        String safeName = originalFileName == null || originalFileName.isBlank() ? "ConvertLab_Edited" : originalFileName;
        String nameWithoutExt = safeName.replaceFirst("[.][^.]+$", "");
        return nameWithoutExt + "_edited.pdf";
    }
}
