package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.AnalyticsService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.PageVisitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/page-visit")
    public ResponseEntity<ApiResponse<String>> recordPageVisit(
            @RequestBody PageVisitRequest request) {
        log.info("Recording page visit for path: {}", request.getPath());

        try {
            analyticsService.recordPageVisit(request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("Error recording page visit for path: {}", request.getPath(), e);
            String error = "Error recording page visit for path: " + request.getPath();
            return ResponseEntity.ok(ApiResponse.failure(error,"ERROR"));
        }
    }
}
