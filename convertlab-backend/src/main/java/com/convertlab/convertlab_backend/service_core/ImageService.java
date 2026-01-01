package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.ImageToPdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ImageToPdfRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Log4j2
@Service
public class ImageService {

    private final StorageService storageService;

    public ImageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public UploadResponse uploadImage(MultipartFile file) throws Exception {
        log.debug("Starting image upload process for file: {}", file.getOriginalFilename());

        String assetId = storageService.saveTempImage(file);
        log.debug("Image saved with assetId: {}", assetId);

        File savedFile = storageService.loadImage(assetId);
        BufferedImage image = ImageIO.read(savedFile);

        int width = image.getWidth();
        int height = image.getHeight();

        return new UploadResponse(
                assetId,
                0, // pageCount not applicable for images
                file.getOriginalFilename(),
                "temp"
        );
    }

    public ExtractedFile convertImagesToPdf(ImageToPdfRequest request) throws Exception {
        log.info("Starting image to PDF conversion for {} images", request.getImages().size());

        byte[] pdfBytes = ImageToPdfUtils.convertImagesToPdf(
                request.getImages(),
                storageService
        );

        log.info("Images converted to PDF successfully, output size: {} bytes", pdfBytes.length);

        return new ExtractedFile(pdfBytes, "ConvertLab_Images.pdf");
    }
}