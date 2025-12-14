package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final StorageService storageService;



    public String generateThumbnail(File file, String assetId) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);
            return storageService.saveThumbnail(assetId, image);
        }
    }
}
