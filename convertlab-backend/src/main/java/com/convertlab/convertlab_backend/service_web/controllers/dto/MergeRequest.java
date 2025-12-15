package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Data;
import java.util.List;

@Data
public class MergeRequest {
    private List<String> fileIds;
}