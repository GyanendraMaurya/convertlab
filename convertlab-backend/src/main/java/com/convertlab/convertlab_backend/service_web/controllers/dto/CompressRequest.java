package com.convertlab.convertlab_backend.service_web.controllers.dto;

import com.convertlab.convertlab_backend.api.enums.CompressionLevel;
import lombok.Data;
import java.util.List;

@Data
public class CompressRequest {
    private List<String> fileIds;
    private CompressionLevel compressionLevel;
}