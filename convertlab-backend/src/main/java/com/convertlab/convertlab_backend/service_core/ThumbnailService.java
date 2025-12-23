package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;

@Log4j2
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final StorageService storageService;

    public String generateThumbnail(File file, String assetId) throws Exception {
        log.debug("Generating thumbnail for assetId: {}, file: {}", assetId, file.getName());

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150);

            String thumbnailPath = storageService.saveThumbnail(assetId, image);

            log.info("Thumbnail generated successfully for assetId: {}", assetId);

            return thumbnailPath;
        } catch (Exception e) {
            log.error("Error generating thumbnail for assetId: {}", assetId, e);
            throw e;
        }
    }
}