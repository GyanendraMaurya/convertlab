package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.VersionResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Log4j2
@RestController
@RequestMapping("/version")
public class VersionController {

    @Value("${app.version:0.0.0}")
    private String version;

    @Value("${app.build-time:Unknown}")
    private String buildTime;

    @GetMapping
    public ResponseEntity<ApiResponse<VersionResponse>> getVersion() {
        log.info("Version endpoint accessed");

        VersionResponse versionResponse = VersionResponse.builder()
                .version(version)
                .buildTime(buildTime)
                .serverTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.ok(ApiResponse.success(versionResponse));
    }
}