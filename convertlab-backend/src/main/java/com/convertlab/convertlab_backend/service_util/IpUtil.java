package com.convertlab.convertlab_backend.service_util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Log4j2
public class IpUtil {

    /**
     * Hash an IP address using SHA-256
     * This makes the IP non-reversible while still allowing uniqueness tracking
     *
     * @param ipAddress The IP address to hash
     * @return Hexadecimal hash string, or null if hashing fails
     */
    public static String hashIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Attempted to hash null or blank IP address");
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));

            // Convert to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hash = hexString.toString();
            log.debug("IP hashed successfully: {} -> {}",
                    maskIp(ipAddress), hash.substring(0, 16) + "...");

            return hash;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }

    /**
     * Mask IP address for logging,etcc (privacy-friendly)
     * Example: 192.168.1.100 -> 192.168.x.x
     */
    public static String maskIp(String ip) {
        if (ip == null || !ip.contains(".")) {
            return "unknown";
        }

        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".x.x";
        }

        return "unknown";
    }

    public static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can contain multiple IPs: "client, proxy1, proxy2"
            // First one is the original client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}