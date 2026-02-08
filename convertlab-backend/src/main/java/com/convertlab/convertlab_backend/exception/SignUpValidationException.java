package com.convertlab.convertlab_backend.exception;

public class SignUpValidationException extends RuntimeException {
    private final String code;

    public SignUpValidationException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
