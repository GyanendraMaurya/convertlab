package com.convertlab.convertlab_backend.service_web.controllers.dto;


import com.convertlab.convertlab_backend.api.enums.ActionType;
import lombok.Data;

import java.util.List;

@Data
public class ExtractRequest {
    private String fileId;
    private String pageRange;
    private ActionType actionType;

}

