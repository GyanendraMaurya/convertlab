package com.convertlab.convertlab_backend.service_web.controllers.dto;


import lombok.Data;

import java.util.List;

@Data
public class ExtractRequest {
    private String fileId;
    private String pagesToKeep;

}

