package com.convertlab.convertlab_backend.service_storage;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface StorageService {
    String saveTempPdf(MultipartFile file) throws Exception;
    File loadPdf(String fileId);
    File loadThumbnail(String fileId);
    void delete(String fileId);
    String saveThumbnail(String assetId, BufferedImage image) throws IOException;
}

