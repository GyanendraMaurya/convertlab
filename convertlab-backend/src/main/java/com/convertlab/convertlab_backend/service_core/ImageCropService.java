package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.exception.FileValidationException;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Log4j2
@Service
@RequiredArgsConstructor
public class ImageCropService {

    private final StorageService storageService;

    /**
     * Processing order:
     *  1. Load original image at full resolution
     *  2. Apply flip (horizontal / vertical)
     *  3. Apply rotation (0 / 90 / 180 / 270)
     *  4. Crop (coordinates are in post-rotation space, natural pixels)
     *  5. Encode to JPEG or PNG
     */
    public ExtractedFile cropAndTransform(
            String fileId,
            int cropX, int cropY, int cropWidth, int cropHeight,
            int rotation,
            boolean flipHorizontal, boolean flipVertical,
            String outputFormat, int quality
    ) throws IOException {

        log.info("Crop request — fileId: {}, crop: ({},{},{},{}), rotation: {}, flipH: {}, flipV: {}",
                fileId, cropX, cropY, cropWidth, cropHeight, rotation, flipHorizontal, flipVertical);

        File imageFile = storageService.loadImage(fileId);
        if (imageFile == null || !imageFile.exists()) {
            throw new FileValidationException("Image file not found: " + fileId, "IMAGE_NOT_FOUND");
        }

        BufferedImage original = ImageIO.read(imageFile);
        if (original == null) {
            throw new FileValidationException("Cannot read image file: " + fileId, "IMAGE_READ_FAILED");
        }

        BufferedImage current = applyFlip(original, flipHorizontal, flipVertical);

        current = applyRotation(current, rotation);

        int imgW = current.getWidth();
        int imgH = current.getHeight();

        int safeX = Math.max(0, Math.min(cropX, imgW - 1));
        int safeY = Math.max(0, Math.min(cropY, imgH - 1));
        int safeW = Math.max(1, Math.min(cropWidth,  imgW - safeX));
        int safeH = Math.max(1, Math.min(cropHeight, imgH - safeY));

        if (safeW != cropWidth || safeH != cropHeight) {
            log.warn("Crop region clamped from ({},{},{},{}) to ({},{},{},{})",
                    cropX, cropY, cropWidth, cropHeight, safeX, safeY, safeW, safeH);
        }

        BufferedImage cropped = current.getSubimage(safeX, safeY, safeW, safeH);

        // Force a deep copy so the subimage is independent of the parent raster
        BufferedImage result = deepCopy(cropped);

        String format = (outputFormat != null && outputFormat.equalsIgnoreCase("PNG")) ? "PNG" : "JPEG";
        byte[] bytes = encode(result, format, quality);

        String originalFileName = getOriginalFileName(imageFile);
        String nameWithoutExt   = originalFileName.replaceFirst("[.][^.]+$", "");
        String ext               = format.equalsIgnoreCase("PNG") ? ".png" : ".jpg";
        String outputFileName    = nameWithoutExt + "_cropped" + ext;

        log.info("Crop complete — output: {}, size: {} bytes, dimensions: {}×{}",
                outputFileName, bytes.length, result.getWidth(), result.getHeight());

        return new ExtractedFile(bytes, outputFileName);
    }

    private BufferedImage applyFlip(BufferedImage img, boolean flipH, boolean flipV) {
        if (!flipH && !flipV) return img;

        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, toRgbType(img.getType()));
        Graphics2D g = out.createGraphics();
        applyRenderingHints(g);

        AffineTransform at = new AffineTransform();
        if (flipH && flipV) {
            at.translate(w, h);
            at.scale(-1, -1);
        } else if (flipH) {
            at.translate(w, 0);
            at.scale(-1, 1);
        } else {
            at.translate(0, h);
            at.scale(1, -1);
        }
        g.drawImage(img, at, null);
        g.dispose();
        return out;
    }

    private BufferedImage applyRotation(BufferedImage img, int degrees) {
        degrees = ((degrees % 360) + 360) % 360; // normalise to 0–359
        if (degrees == 0) return img;

        int srcW = img.getWidth();
        int srcH = img.getHeight();

        // Swap dimensions for 90 / 270
        int dstW = (degrees == 90 || degrees == 270) ? srcH : srcW;
        int dstH = (degrees == 90 || degrees == 270) ? srcW : srcH;

        BufferedImage out = new BufferedImage(dstW, dstH, toRgbType(img.getType()));
        Graphics2D g = out.createGraphics();
        applyRenderingHints(g);

        g.translate(dstW / 2.0, dstH / 2.0);
        g.rotate(Math.toRadians(degrees));
        g.translate(-srcW / 2.0, -srcH / 2.0);
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage deepCopy(BufferedImage src) {
        int type = toRgbType(src.getType());
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), type);
        Graphics2D g = copy.createGraphics();
        applyRenderingHints(g);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    /** Ensure we always use a concrete RGB type — avoids IndexColorModel issues */
    private int toRgbType(int original) {
        if (original == BufferedImage.TYPE_INT_ARGB || original == BufferedImage.TYPE_4BYTE_ABGR) {
            return BufferedImage.TYPE_INT_ARGB;
        }
        return BufferedImage.TYPE_INT_RGB;
    }

    private void applyRenderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private byte[] encode(BufferedImage img, String format, int quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if ("PNG".equalsIgnoreCase(format)) {
                // PNG is lossless — no quality param
                ImageIO.write(img, "png", baos);
            } else {
                // JPEG with configurable quality
                // JPEG doesn't support alpha — convert to RGB if needed
                if (img.getColorModel().hasAlpha()) {
                    BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = rgb.createGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, img.getWidth(), img.getHeight());
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                    img = rgb;
                }

                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) throw new IOException("No JPEG writer found");
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();

                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    float q = Math.max(0.01f, Math.min(1.0f, quality / 100.0f));
                    param.setCompressionQuality(q);
                }

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(img, null, null), param);
                } finally {
                    writer.dispose();
                }
            }
            return baos.toByteArray();
        }
    }

    private String getOriginalFileName(File imageFile) {
        if (imageFile == null) return "image";
        String name = imageFile.getName();
        String[] parts = name.split("_", 2);
        return parts.length > 1 ? parts[1] : name;
    }
}