package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.api.enums.ActionType;
import com.convertlab.convertlab_backend.service_core.PdfService;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_storage.StorageService;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.MergeRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final StorageService storageService;


    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UploadResponse>> upload(@RequestParam MultipartFile file) throws Exception {
        System.out.println(file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.success(pdfService.uploadPdf(file)));
    }

    @PostMapping("/extract")
    public ResponseEntity<Resource> extract(@RequestBody ExtractRequest request) throws Exception {
        PdfUtils.validateInputRangePattern(request.getPageRange());
        File pdfFile = storageService.loadPdf(request.getFileId());
        int totalPages = PdfUtils.getPageCount(pdfFile);
        List<Integer> pagesToKeep = PdfUtils.getPageRanges(request.getPageRange(), totalPages, request.getActionType().equals(ActionType.KEEP));
        ExtractedFile extractedFile = pdfService.extractPages(request, pagesToKeep);

        ByteArrayResource resource = new ByteArrayResource(extractedFile.getFileBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + "extracted_" + extractedFile.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(extractedFile.getFileBytes().length)
                .body(resource);
    }

    @GetMapping("/thumbnail/{assetId}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String assetId) throws Exception {
        File image = storageService.loadThumbnail(assetId);
        byte[] bytes = Files.readAllBytes(image.toPath());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    @PostMapping("/merge")
    public ResponseEntity<Resource> merge(@RequestBody MergeRequest request) throws Exception {
        ExtractedFile mergedFile = pdfService.mergePdfs(request);

        ByteArrayResource resource = new ByteArrayResource(mergedFile.getFileBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + mergedFile.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(mergedFile.getFileBytes().length)
                .body(resource);
    }

}
