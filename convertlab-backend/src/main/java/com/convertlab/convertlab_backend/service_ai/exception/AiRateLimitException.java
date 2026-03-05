package com.convertlab.convertlab_backend.service_ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AiRateLimitException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public AiRateLimitException(String message, String code, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
