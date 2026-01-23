package com.convertlab.convertlab_backend.service_util;

import com.convertlab.convertlab_backend.api.enums.CompressionLevel;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Log4j2
public class PdfCompressionUtils {

    /**
     * Compress a PDF file based on the compression level
     *
     * @param pdfFile          The source PDF file
     * @param compressionLevel The compression level (LOW, MEDIUM, HIGH)
     * @return Compressed PDF as byte array
     * @throws IOException If an error occurs during compression
     */
    public static byte[] compressPdf(File pdfFile, CompressionLevel compressionLevel) throws IOException {
        log.info("Starting PDF compression for file: {} with level: {}",
                pdfFile.getName(), compressionLevel);

        try (PDDocument document = Loader.loadPDF(pdfFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Get compression quality based on level
            float imageQuality = getImageQuality(compressionLevel);
            int imageDPI = getImageDPI(compressionLevel);

            log.debug("Compression settings - Quality: {}, DPI: {}", imageQuality, imageDPI);

            // Process each page
            for (PDPage page : document.getPages()) {
                compressPageImages(document, page, imageQuality, imageDPI);
            }

            // Save compressed document
            document.save(baos);

            byte[] compressedBytes = baos.toByteArray();
            long originalSize = pdfFile.length();
            long compressedSize = compressedBytes.length;
            float compressionRatio = ((float) (originalSize - compressedSize) / originalSize) * 100;

            log.info("Compression completed - Original: {} bytes, Compressed: {} bytes, Savings: {}%",
                    originalSize, compressedSize, compressionRatio);

            return compressedBytes;
        }
    }

    /**
     * Compress images in a PDF page
     */
    private static void compressPageImages(
            PDDocument document,
            PDPage page,
            float quality,
            int targetDPI
    ) throws IOException {

        PDResources resources = page.getResources();
        if (resources == null) {
            return;
        }

        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);

            if (!(xObject instanceof PDImageXObject image)) {
                continue;
            }

            try {
                BufferedImage bufferedImage = image.getImage();

                // Skip tiny images (icons, masks, etc.)
                if (bufferedImage.getWidth() < 100 || bufferedImage.getHeight() < 100) {
                    continue;
                }

                BufferedImage resizedImage = resizeImage(bufferedImage, targetDPI);
                byte[] compressedImageData = compressImage(resizedImage, quality);

                PDImageXObject compressedImage =
                        PDImageXObject.createFromByteArray(
                                document,
                                compressedImageData,
                                name.getName()
                        );

                resources.put(name, compressedImage);

            } catch (Exception e) {
                log.warn("Failed to compress image '{}' on page: {}",
                        name.getName(), e.getMessage());
            }
        }
    }

    /**
     * Resize image based on target DPI
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetDPI) {
        // Calculate scale factor based on DPI
        // Assuming 150 DPI as baseline for typical PDF images
        float scaleFactor = targetDPI / 150.0f;

        if (scaleFactor >= 1.0f) {
            return original; // No downscaling needed
        }

        int newWidth = (int) (original.getWidth() * scaleFactor);
        int newHeight = (int) (original.getHeight() * scaleFactor);

        // Ensure minimum dimensions
        newWidth = Math.max(newWidth, 100);
        newHeight = Math.max(newHeight, 100);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
        var graphics = resized.createGraphics();
        graphics.drawImage(original, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        return resized;
    }

    /**
     * Compress image using JPEG compression
     */
    private static byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * Get image quality based on compression level
     */
    private static float getImageQuality(CompressionLevel level) {
        return switch (level) {
            case LOW -> 0.9f;      // Minimal compression
            case MEDIUM -> 0.7f;   // Moderate compression
            case HIGH -> 0.5f;     // Maximum compression
        };
    }

    /**
     * Get target DPI based on compression level
     */
    private static int getImageDPI(CompressionLevel level) {
        return switch (level) {
            case LOW -> 150;       // Keep original resolution
            case MEDIUM -> 120;    // Reduce resolution moderately
            case HIGH -> 96;       // Reduce resolution significantly
        };
    }
}