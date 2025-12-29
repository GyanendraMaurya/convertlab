package com.convertlab.convertlab_backend.service_web.controllers.dto;

import com.convertlab.convertlab_backend.api.enums.SplitType;
import lombok.Data;

@Data
public class SplitRequest {
    private String fileId;
    private String pageRange;
    private SplitType splitType;
}