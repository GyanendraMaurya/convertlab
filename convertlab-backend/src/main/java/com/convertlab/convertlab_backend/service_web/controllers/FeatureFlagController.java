package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_core.FeatureFlagService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/features")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> getPublicFeatures() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.getPublicFeatures()));
    }
}
