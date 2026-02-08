package com.convertlab.convertlab_backend.security_util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordUtil {

    private final PasswordEncoder passwordEncoder;

    public String hash(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean matches(String raw, String hashed) {
        return passwordEncoder.matches(raw, hashed);
    }
}

