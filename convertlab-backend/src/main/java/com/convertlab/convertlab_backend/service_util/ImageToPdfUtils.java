package com.convertlab.convertlab_backend.service_util;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ImageToPdfRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageToPdfUtils {

    public static byte[] convertImagesToPdf(
            List<ImageToPdfRequest.ImageInfo> images,
            StorageService storageService
    ) throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (ImageToPdfRequest.ImageInfo imageInfo : images) {
                File imageFile = storageService.loadImage(imageInfo.getFileId());
                BufferedImage bufferedImage = ImageIO.read(imageFile);

                // Apply rotation if needed
                if (imageInfo.getRotation() != 0) {
                    bufferedImage = rotateImage(bufferedImage, imageInfo.getRotation());
                }

                // Create temporary file for rotated image
                File tempFile = File.createTempFile("rotated_", ".png");
                ImageIO.write(bufferedImage, "png", tempFile);

                // Create PDF page with image dimensions
                float width = bufferedImage.getWidth();
                float height = bufferedImage.getHeight();
                PDRectangle pageSize = new PDRectangle(width, height);
                PDPage page = new PDPage(pageSize);
                document.addPage(page);

                // Add image to page
                PDImageXObject pdImage = PDImageXObject.createFromFile(
                        tempFile.getAbsolutePath(),
                        document
                );

                try (PDPageContentStream contentStream = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                    contentStream.drawImage(pdImage, 0, 0, width, height);
                }

                // Clean up temp file
                tempFile.delete();
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    private static BufferedImage rotateImage(BufferedImage image, int rotation) {
        double radians = Math.toRadians(rotation);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) Math.floor(image.getWidth() * cos + image.getHeight() * sin);
        int newHeight = (int) Math.floor(image.getHeight() * cos + image.getWidth() * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        AffineTransform transform = new AffineTransform();
        transform.translate((newWidth - image.getWidth()) / 2.0, (newHeight - image.getHeight()) / 2.0);

        int x = image.getWidth() / 2;
        int y = image.getHeight() / 2;

        transform.rotate(radians, x, y);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);

        return op.filter(image, rotated);
    }
}