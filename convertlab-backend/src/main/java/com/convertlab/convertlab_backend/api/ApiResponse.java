package com.convertlab.convertlab_backend.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ApiError error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static ApiResponse<?> failure(String message, String code) {
        return ApiResponse.builder()
                .success(false)
                .error(new ApiError(message, code))
                .build();
    }
}

