package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ImageUploadResponse {
    final private String fileId;
    final private String fileName;
}
