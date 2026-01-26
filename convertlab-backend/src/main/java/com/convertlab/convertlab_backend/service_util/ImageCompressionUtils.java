package com.convertlab.convertlab_backend.service_util;

import com.convertlab.convertlab_backend.api.enums.CompressionLevel;
import lombok.extern.log4j.Log4j2;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Log4j2
public class ImageCompressionUtils {

    /**
     * Compress an image file based on the compression level
     *
     * @param imageFile        The source image file
     * @param compressionLevel The compression level (LOW, MEDIUM, HIGH)
     * @return Compressed image as byte array
     * @throws IOException If an error occurs during compression
     */
    public static byte[] compressImage(File imageFile, CompressionLevel compressionLevel) throws IOException {
        log.info("Starting image compression for file: {} with level: {}",
                imageFile.getName(), compressionLevel);

        BufferedImage originalImage = ImageIO.read(imageFile);

        if (originalImage == null) {
            throw new IOException("Unable to read image file: " + imageFile.getName());
        }

        long originalSize = imageFile.length();

        // Get compression settings based on level
        float quality = getCompressionQuality(compressionLevel);
        float scaleFactor = getScaleFactor(compressionLevel);

        log.debug("Compression settings - Quality: {}, Scale Factor: {}", quality, scaleFactor);

        // Resize image if needed
        BufferedImage processedImage = originalImage;
        if (scaleFactor < 1.0f) {
            processedImage = resizeImage(originalImage, scaleFactor);
        }

        // Compress to JPEG
        byte[] compressedBytes = compressToJpeg(processedImage, quality);

        long compressedSize = compressedBytes.length;
        float compressionRatio = ((float) (originalSize - compressedSize) / originalSize) * 100;

        log.info("Compression completed - Original: {} bytes, Compressed: {} bytes, Savings: {}%",
                originalSize, compressedSize, String.format("%.2f", compressionRatio));

        return compressedBytes;
    }

    /**
     * Resize image based on scale factor
     */
    private static BufferedImage resizeImage(BufferedImage original, float scaleFactor) {
        int newWidth = (int) (original.getWidth() * scaleFactor);
        int newHeight = (int) (original.getHeight() * scaleFactor);

        // Ensure minimum dimensions
        newWidth = Math.max(newWidth, 100);
        newHeight = Math.max(newHeight, 100);

        log.debug("Resizing image from {}x{} to {}x{}",
                original.getWidth(), original.getHeight(), newWidth, newHeight);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();

        // Use high-quality rendering
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.drawImage(original, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        return resized;
    }

    /**
     * Compress image to JPEG format with specified quality
     */
    private static byte[] compressToJpeg(BufferedImage image, float quality) throws IOException {
        // Convert to RGB if needed (JPEG doesn't support alpha)
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

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
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * Get compression quality based on compression level
     */
    private static float getCompressionQuality(CompressionLevel level) {
        return switch (level) {
            case LOW -> 0.9f;      // Minimal compression, highest quality
            case MEDIUM -> 0.75f;  // Moderate compression
            case HIGH -> 0.6f;     // Maximum compression, lower quality
        };
    }

    /**
     * Get scale factor based on compression level
     */
    private static float getScaleFactor(CompressionLevel level) {
        return switch (level) {
            case LOW -> 1.0f;      // Keep original dimensions
            case MEDIUM -> 0.85f;  // Reduce dimensions by 15%
            case HIGH -> 0.7f;     // Reduce dimensions by 30%
        };
    }

    /**
     * Get file extension based on original filename
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}