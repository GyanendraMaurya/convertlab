package com.convertlab.convertlab_backend.service_web.controllers.dto;

import com.convertlab.convertlab_backend.api.enums.PdfPasswordAction;
import lombok.Data;

@Data
public class PdfPasswordRequest {
    private String fileId;
    private String password;
    private PdfPasswordAction action; // ADD or REMOVE
}
