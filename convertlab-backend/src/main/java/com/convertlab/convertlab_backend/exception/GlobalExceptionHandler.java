package com.convertlab.convertlab_backend.exception;

import com.convertlab.convertlab_backend.api.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxUploadSize;

    @ExceptionHandler(InvalidPageInputException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidPageInput(InvalidPageInputException ex) {
        log.warn("Invalid page input: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(ex.getMessage(), "INVALID_PAGE_INPUT"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralError(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred", "INTERNAL_ERROR"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        String msg = "Uploaded file is too large. Max allowed size is " + maxUploadSize + ".";
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(ApiResponse.failure(msg, "MAX_UPLOAD_EXCEEDED"));
    }
}