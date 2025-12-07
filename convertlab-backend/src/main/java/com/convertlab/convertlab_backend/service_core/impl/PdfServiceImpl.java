package com.convertlab.convertlab_backend.service_core.impl;

import com.convertlab.convertlab_backend.exception.InvalidPageInputException;
import com.convertlab.convertlab_backend.service_core.PdfService;
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
public class PdfServiceImpl implements PdfService {

    private final StorageService storageService;

    public PdfServiceImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public UploadResponse uploadPdf(MultipartFile file) throws Exception {
//        throw new InvalidPageInputException("This is some exception");
        String fileId = storageService.saveTemp(file);
        File savedFile = storageService.load(fileId);

        int pageCount = PdfUtils.getPageCount(savedFile);

        return new UploadResponse(fileId, pageCount);
    }

    @Override
    public ExtractedFile extractPages(ExtractRequest request, List<Integer> pagesToKeep) throws Exception {
        File pdfFile = storageService.load(request.getFileId());
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);
        byte[] fileBytes = PdfUtils.extractPages(pdfFile, pagesToKeep);
        return new ExtractedFile(fileBytes, originalFileName);
    }

    @Override
    public ExtractedFile extractPages(File file, List<Integer> pagesToKeep) throws Exception {
        String originalFileName = PdfUtils.getOriginalUserFileName(file);
        byte[] fileBytes = PdfUtils.extractPages(file, pagesToKeep);
        return new ExtractedFile(fileBytes, originalFileName);
    }
}
