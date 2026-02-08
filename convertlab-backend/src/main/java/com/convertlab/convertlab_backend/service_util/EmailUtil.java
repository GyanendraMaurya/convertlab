package com.convertlab.convertlab_backend.service_util;

import org.apache.commons.validator.routines.EmailValidator;

public final class EmailUtil {

    private static final EmailValidator VALIDATOR =
            EmailValidator.getInstance();

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return VALIDATOR.isValid(email);
    }
}

