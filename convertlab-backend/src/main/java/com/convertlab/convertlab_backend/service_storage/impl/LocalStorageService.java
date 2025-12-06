package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path root = Paths.get("temp-files");

    public LocalStorageService() throws IOException {
        Files.createDirectories(root);
    }

    @Override
    public String saveTemp(MultipartFile file) throws Exception {
        String id = UUID.randomUUID().toString();
        String name =  id + "_" + file.getOriginalFilename();
        Path dest = root.resolve(name);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    @Override
    public File load(String fileId) {
        return root.resolve(fileId).toFile();
    }

    @Override
    public void delete(String fileId) {
        try {
            Files.deleteIfExists(root.resolve(fileId + ".pdf"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
