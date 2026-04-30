package com.convertlab.convertlab_backend.exception;

import lombok.Getter;

@Getter
public class ContactInquiryValidationException extends RuntimeException {
    private final String code;

    public ContactInquiryValidationException(String message, String code) {
        super(message);
        this.code = code;
    }
}
