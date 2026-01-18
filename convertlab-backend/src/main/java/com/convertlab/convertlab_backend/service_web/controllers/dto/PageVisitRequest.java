package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
public class PageVisitRequest {
    private UUID sessionId;
    private String path;
    private Boolean entry;
}
