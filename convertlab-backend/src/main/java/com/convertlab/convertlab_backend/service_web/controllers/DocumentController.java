package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.service_ai.PdfTextExtractionService;
import com.convertlab.convertlab_backend.service_ai.RagService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractTextRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.QueryRequest;
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

    private final PdfTextExtractionService pdfTextExtractionService;
    private final RagService ragService;

    @PostMapping("/ingest")
    public ResponseEntity<String> extractText(@RequestBody ExtractTextRequest request) {
        ragService.ingest(request.fileId());
        return ResponseEntity.ok("");
    }

    @PostMapping("/query")
    public ResponseEntity<String> query(@RequestBody QueryRequest request) {
        String result = ragService.answerQuery(request.fileId(), request.query());
        return ResponseEntity.ok(result);
    }


}