package com.convertlab.convertlab_backend.exception;

public class PdfPasswordException extends RuntimeException {
    private final String code;

    public PdfPasswordException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
