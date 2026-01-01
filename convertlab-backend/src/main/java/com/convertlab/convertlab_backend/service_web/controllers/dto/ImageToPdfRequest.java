package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImageToPdfRequest {
    private List<ImageInfo> images;

    @Data
    public static class ImageInfo {
        private String fileId;
        private int rotation; // 0, 90, 180, 270
    }
}