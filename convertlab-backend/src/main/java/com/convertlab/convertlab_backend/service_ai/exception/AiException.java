package com.convertlab.convertlab_backend.service_ai.exception;

import org.springframework.http.HttpStatus;

public class AiException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;

    public AiException(String message, String code) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    }

    public AiException(String message, String code, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;

    }

    public AiException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public String getCode() {
        return code;
    }
}