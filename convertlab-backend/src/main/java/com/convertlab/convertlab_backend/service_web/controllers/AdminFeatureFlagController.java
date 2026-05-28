package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.FeatureFlagService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagBulkUpdateRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/features")
@RequiredArgsConstructor
public class AdminFeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.getAllFeatures()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> updateFeatures(
            @RequestBody FeatureFlagBulkUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.updateFeatures(request)));
    }
}
