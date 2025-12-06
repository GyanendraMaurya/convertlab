package com.convertlab.convertlab_backend.service_storage;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

public interface StorageService {
    String saveTemp(MultipartFile file) throws Exception;
    File load(String fileId);
    void delete(String fileId);
}

