package com.convertlab.convertlab_backend.exception;

import com.convertlab.convertlab_backend.api.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidPageInputException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidPageInput(InvalidPageInputException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(ex.getMessage(), "INVALID_PAGE_INPUT"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralError(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred", "INTERNAL_ERROR"));
    }
}


