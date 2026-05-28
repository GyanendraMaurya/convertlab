package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.entity.FeatureFlag;
import com.convertlab.convertlab_backend.exception.FeatureDisabledException;
import com.convertlab.convertlab_backend.repository.FeatureFlagRepository;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagBulkUpdateRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.FeatureFlagUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    public static final String SHOW_CONTACT_PAGE = "SHOW_CONTACT_PAGE";

    private final FeatureFlagRepository featureFlagRepository;

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> getPublicFeatures() {
        return featureFlagRepository.findAllByExposeToFrontendTrueOrderByTitleAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> getAllFeatures() {
        return featureFlagRepository.findAllByOrderByTitleAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<FeatureFlagResponse> updateFeatures(FeatureFlagBulkUpdateRequest request) {
        if (request == null || request.features() == null) {
            throw new IllegalArgumentException("Feature updates are required");
        }

        List<FeatureFlagUpdateRequest> updates = request.features();
        if (updates.stream().anyMatch(update -> update == null || update.code() == null || update.code().isBlank())) {
            throw new IllegalArgumentException("Feature code is required");
        }

        Map<String, FeatureFlagUpdateRequest> updatesByCode = updates.stream()
                .collect(Collectors.toMap(
                        FeatureFlagUpdateRequest::code,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        List<FeatureFlag> existingFlags = featureFlagRepository.findAllById(updatesByCode.keySet());
        HashSet<String> existingCodes = existingFlags.stream()
                .map(FeatureFlag::getCode)
                .collect(Collectors.toCollection(HashSet::new));

        List<String> unknownCodes = updatesByCode.keySet()
                .stream()
                .filter(code -> !existingCodes.contains(code))
                .toList();

        if (!unknownCodes.isEmpty()) {
            throw new IllegalArgumentException("Unknown feature code: " + unknownCodes.getFirst());
        }

        existingFlags.forEach(flag -> flag.setEnabled(updatesByCode.get(flag.getCode()).enabled()));
        featureFlagRepository.saveAll(existingFlags);

        return getAllFeatures();
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(String code) {
        return featureFlagRepository.findById(code)
                .map(FeatureFlag::isEnabled)
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public void requireEnabled(String code, String message) {
        if (!isEnabled(code)) {
            throw new FeatureDisabledException(message);
        }
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        return new FeatureFlagResponse(
                flag.getCode(),
                flag.getTitle(),
                flag.isEnabled(),
                flag.isExposeToFrontend()
        );
    }
}
