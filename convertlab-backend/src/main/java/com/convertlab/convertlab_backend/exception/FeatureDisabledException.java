package com.convertlab.convertlab_backend.exception;

import lombok.Getter;

@Getter
public class FeatureDisabledException extends RuntimeException {

    private final String code;

    public FeatureDisabledException(String message) {
        super(message);
        this.code = "FEATURE_DISABLED";
    }
}
