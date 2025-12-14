package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UploadResponse {
    final private String fileId;
    final private int pageCount;
    final private String fileName;
    final private String thumbnailUrl;

}
