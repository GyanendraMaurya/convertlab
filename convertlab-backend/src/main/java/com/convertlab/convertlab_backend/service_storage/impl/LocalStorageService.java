package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.StorageService;
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

@Service
public class LocalStorageService implements StorageService {

    private final Path pdfDir = Paths.get("temp-files/pdf");
    private final Path thumbnailDir = Paths.get("temp-files/thumbnail");


    public LocalStorageService() throws IOException {
        Files.createDirectories(pdfDir);
        Files.createDirectories(thumbnailDir);
    }

    @Override
    public String saveTempPdf(MultipartFile file) throws Exception {
        String id = UUID.randomUUID().toString();
        String name =  id + "_" + file.getOriginalFilename();
        Path dest = pdfDir.resolve(name);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    @Override
    public File loadPdf(String fileId) {
        return pdfDir.resolve(fileId).toFile();
    }

    public File loadThumbnail(String fileId) {
        return thumbnailDir.resolve(fileId).toFile();
    }

    @Override
    public void delete(String fileId) {
        try {
            Files.deleteIfExists(pdfDir.resolve(fileId + ".pdf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String saveThumbnail(String assetId, BufferedImage image) throws IOException {
        Path output  = thumbnailDir.resolve(assetId+".png");
        ImageIO.write(image, "png", output.toFile());
        return thumbnailDir.getFileName()+"/" + output.getFileName();
    }
}
