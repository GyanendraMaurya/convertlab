package com.convertlab.convertlab_backend.service_core.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExtractedFile {
    private byte[] fileBytes;
    private String fileName;
}
