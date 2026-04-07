package com.convertlab.convertlab_backend.api.enums;

public enum AuthProviders {
    LOCAL("local"),
    GOOGLE("GOOGLE");

    private final String value;

    AuthProviders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
