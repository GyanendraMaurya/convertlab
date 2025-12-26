package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.MergeRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Service
public class PdfService {

    private final StorageService storageService;
    private final ThumbnailService thumbnailService;

    public PdfService(StorageService storageService, ThumbnailService thumbnailService) {
        this.storageService = storageService;
        this.thumbnailService = thumbnailService;
    }

    public UploadResponse uploadPdf(MultipartFile file) throws Exception {
        log.debug("Starting PDF upload process for file: {}", file.getOriginalFilename());

        String assetId = storageService.saveTempPdf(file);
        log.debug("File saved with assetId: {}", assetId);

        File savedFile = storageService.loadPdf(assetId);
        int pageCount = PdfUtils.getPageCount(savedFile);
        log.debug("PDF has {} pages", pageCount);


        return new UploadResponse(
                assetId,
                pageCount,
                file.getOriginalFilename(),
                "temp"
        );
    }

    public ExtractedFile extractPages(ExtractRequest request, List<Integer> pagesToKeep) throws Exception {
        log.debug("Extracting pages from fileId: {}, pages to keep: {}",
                request.getFileId(), pagesToKeep);

        File pdfFile = storageService.loadPdf(request.getFileId());
        String originalFileName = PdfUtils.getOriginalUserFileName(pdfFile);
        byte[] fileBytes = PdfUtils.extractPages(pdfFile, pagesToKeep);

        log.debug("Extracted {} pages, output size: {} bytes",
                pagesToKeep.size(), fileBytes.length);

        return new ExtractedFile(fileBytes, originalFileName);
    }

    public ExtractedFile extractPages(File file, List<Integer> pagesToKeep) throws Exception {
        log.debug("Extracting pages from file: {}, pages to keep: {}",
                file.getName(), pagesToKeep);

        String originalFileName = PdfUtils.getOriginalUserFileName(file);
        byte[] fileBytes = PdfUtils.extractPages(file, pagesToKeep);

        log.debug("Extracted {} pages, output size: {} bytes",
                pagesToKeep.size(), fileBytes.length);

        return new ExtractedFile(fileBytes, originalFileName);
    }

    public ExtractedFile mergePdfs(MergeRequest request) throws Exception {
        log.info("Starting PDF merge for {} files", request.getFileIds().size());

        // Load all PDF files based on the file IDs in the request
        List<File> pdfFiles = request.getFileIds().stream()
                .map(fileId -> {
                    log.debug("Loading file for merge: {}", fileId);
                    return storageService.loadPdf(fileId);
                })
                .collect(Collectors.toList());

        // Merge the PDFs using PdfUtils
        byte[] mergedBytes = PdfUtils.mergePdfs(pdfFiles);

        log.info("PDFs merged successfully, output size: {} bytes", mergedBytes.length);

        // Return the merged PDF with the standard filename
        return new ExtractedFile(mergedBytes, "ConvertLab_Merge.pdf");
    }
}