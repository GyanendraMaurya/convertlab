package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_ai.RagService;
import com.convertlab.convertlab_backend.service_ai.UserAiUsageService;
import com.convertlab.convertlab_backend.service_ai.dto.IngestResponse;
import com.convertlab.convertlab_backend.service_ai.dto.QueryRequest;
import com.convertlab.convertlab_backend.service_ai.dto.QueryResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractTextRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final RagService ragService;
    private final UserAiUsageService userAiUsageService;

    /**
     * Ingest a PDF into the vector store.
     * Requires authentication. Subject to per-user daily limit and per-IP rate limiting.
     *
     * @param principal the authenticated user's email (from JWT subject)
     */
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<IngestResponse>> ingest(
            @RequestBody ExtractTextRequest request,
            @AuthenticationPrincipal String principal
    ) {
        log.info("Ingest request from user: {} for fileId: {}", principal, request.fileId());

        // Enforce per-user daily limit (throws AiRateLimitException if exceeded)
        userAiUsageService.checkAndIncrementIngestLimit(principal);

        int chunkCount = ragService.ingest(request.fileId());
        return ResponseEntity.ok(ApiResponse.success(new IngestResponse(chunkCount)));
    }

    /**
     * Query the vector store with a natural language question.
     * Requires authentication. Subject to per-user daily limit and per-IP rate limiting.
     *
     * @param principal the authenticated user's email (from JWT subject)
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<QueryResponse>> query(
            @RequestBody QueryRequest request,
            @AuthenticationPrincipal String principal
    ) {
        log.info("Query request from user: {} for fileId: {}", principal, request.fileId());

        // Enforce per-user daily limit (throws AiRateLimitException if exceeded)
        userAiUsageService.checkAndIncrementQuery(principal);

        String result = ragService.answerQuery(request.fileId(), request.query());
        return ResponseEntity.ok(ApiResponse.success(new QueryResponse(result)));
    }
}