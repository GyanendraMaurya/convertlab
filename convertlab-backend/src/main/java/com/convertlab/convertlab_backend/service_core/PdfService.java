package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
public class PdfService {

    private final StorageService storageService;
    private final ThumbnailService thumbnailService;

    public PdfService(StorageService storageService, ThumbnailService thumbnailService) {
        this.storageService = storageService;
        this.thumbnailService = thumbnailService;
    }

    public UploadResponse uploadPdf(MultipartFile file) throws Exception {
        String assetId = storageService.saveTempPdf(file);
        File savedFile = storageService.loadPdf(assetId);

        int pageCount = PdfUtils.getPageCount(savedFile);
        String thumbnailUrl = thumbnailService.generateThumbnail(savedFile, assetId);

        return new UploadResponse(
                assetId,
                pageCount,
                file.getOriginalFilename(),
                "/api/pdf/" + thumbnailUrl
        );
    }

    public ExtractedFile extractPages(ExtractRequest request, List<Integer> pagesToKeep) throws Exception {
        File pdfFile = storageService.loadPdf(request.getFileId());
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);
        byte[] fileBytes = PdfUtils.extractPages(pdfFile, pagesToKeep);
        return new ExtractedFile(fileBytes, originalFileName);
    }

    public ExtractedFile extractPages(File file, List<Integer> pagesToKeep) throws Exception {
        String originalFileName = PdfUtils.getOriginalUserFileName(file);
        byte[] fileBytes = PdfUtils.extractPages(file, pagesToKeep);
        return new ExtractedFile(fileBytes, originalFileName);
    }


}
