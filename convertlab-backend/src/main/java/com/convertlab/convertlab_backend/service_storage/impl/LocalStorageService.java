package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Log4j2
@Service
public class LocalStorageService implements StorageService {

    private final Path pdfDir = Paths.get("temp-files/pdf");
    private final Path thumbnailDir = Paths.get("temp-files/thumbnail");

    public LocalStorageService() throws IOException {
        Files.createDirectories(pdfDir);
        Files.createDirectories(thumbnailDir);
        log.info("Storage directories initialized: pdf={}, thumbnail={}",
                pdfDir.toAbsolutePath(), thumbnailDir.toAbsolutePath());
    }

    @Override
    public String saveTempPdf(MultipartFile file) throws Exception {
        String id = UUID.randomUUID().toString();
        String name = id + "_" + file.getOriginalFilename();
        Path dest = pdfDir.resolve(name);

        log.debug("Saving PDF file: {} to {}", file.getOriginalFilename(), dest);

        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        log.info("PDF saved successfully: {} (size: {} bytes)", name, file.getSize());

        return name;
    }

    @Override
    public File loadPdf(String fileId) {
        File file = pdfDir.resolve(fileId).toFile();

        if (!file.exists()) {
            log.warn("PDF file not found: {}", fileId);
        } else {
            log.debug("Loading PDF file: {}", fileId);
        }

        return file;
    }

    public File loadThumbnail(String fileId) {
        File file = thumbnailDir.resolve(fileId).toFile();

        if (!file.exists()) {
            log.warn("Thumbnail file not found: {}", fileId);
        } else {
            log.debug("Loading thumbnail: {}", fileId);
        }

        return file;
    }

    @Override
    public void delete(String fileId) {
        try {
            Path filePath = pdfDir.resolve(fileId + ".pdf");
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("Deleted file: {}", fileId);
            } else {
                log.warn("File not found for deletion: {}", fileId);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", fileId, e);
        }
    }

    public String saveThumbnail(String assetId, BufferedImage image) throws IOException {
        Path output = thumbnailDir.resolve(assetId + ".png");

        log.debug("Saving thumbnail for assetId: {} to {}", assetId, output);

        ImageIO.write(image, "png", output.toFile());

        log.info("Thumbnail saved successfully: {}", assetId);

        return thumbnailDir.getFileName() + "/" + output.getFileName();
    }
}