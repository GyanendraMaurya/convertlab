package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.service_ai.DocumentIngestionService;
import com.convertlab.convertlab_backend.service_ai.DocumentProcessingService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractTextRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;
    private final DocumentIngestionService documentIngestionService;

    @PostMapping("/extract-text")
    public ResponseEntity<String> extractText(@RequestBody ExtractTextRequest request) {

        documentIngestionService.ingestDocument(request.fileId());

        return ResponseEntity.ok("");
    }
}