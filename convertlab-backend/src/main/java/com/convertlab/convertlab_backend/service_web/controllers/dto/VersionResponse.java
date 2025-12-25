package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VersionResponse {
    private String version;
    private String buildTime;
    private String serverTime;
}