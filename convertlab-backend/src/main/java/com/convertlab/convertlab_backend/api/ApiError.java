package com.convertlab.convertlab_backend.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiError {
    private String message;
    private String code; // Example: INVALID_PAGE_RANGE
}
