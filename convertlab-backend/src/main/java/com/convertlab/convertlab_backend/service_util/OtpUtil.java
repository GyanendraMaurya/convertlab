package com.convertlab.convertlab_backend.service_util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class OtpUtil {

    private OtpUtil() {}

    public static String hash(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String rawOtp, String storedHash) {
        return hash(rawOtp).equals(storedHash);
    }

    public static String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}

