package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Getter;

@Getter
public class UploadResponse {
    final private String fileId;
    final private int pageCount;

    public UploadResponse(String fileId, int pageCount) {
        this.fileId = fileId;
        this.pageCount = pageCount;
    }
}
