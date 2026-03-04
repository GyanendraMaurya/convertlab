package com.convertlab.convertlab_backend.service_ai.exception;

import org.springframework.http.HttpStatusCode;

public class DocumentIngestionException extends RuntimeException {
    private final String code;
    private final HttpStatusCode httpStatus;

    public DocumentIngestionException(String message, String code, HttpStatusCode httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public DocumentIngestionException(String message, String code) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatusCode.valueOf(500);
    }

    public DocumentIngestionException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = HttpStatusCode.valueOf(500);
    }

    public String getCode() {
        return code;
    }
}
