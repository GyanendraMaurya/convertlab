package com.convertlab.convertlab_backend.exception;

public class FileValidationException extends RuntimeException {
    private final String code;

    public FileValidationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}