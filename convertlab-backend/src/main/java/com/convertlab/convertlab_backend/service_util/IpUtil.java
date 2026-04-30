package com.convertlab.convertlab_backend.service_util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Log4j2
public class IpUtil {

    private static final InetAddressValidator IP_VALIDATOR = InetAddressValidator.getInstance();
    private static final List<String> LOOPBACK_TRUSTED_CIDRS = List.of("127.0.0.0/8", "::1/128");

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
        return extractClientIp(request, LOOPBACK_TRUSTED_CIDRS);
    }

    public static String extractClientIp(HttpServletRequest request, List<String> trustedProxyCidrs) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded == null || forwarded.isBlank() || !isTrustedProxy(remoteAddr, trustedProxyCidrs)) {
            return remoteAddr;
        }

        // X-Forwarded-For can contain "client, proxy1, proxy2". Walk from the
        // trusted edge inward so a spoofed first value cannot bypass rate limits.
        String[] chain = forwarded.split(",");
        for (int i = chain.length - 1; i >= 0; i--) {
            String candidate = normalizeIp(chain[i]);
            if (candidate == null) {
                continue;
            }
            if (!isTrustedProxy(candidate, trustedProxyCidrs)) {
                return candidate;
            }
        }

        return remoteAddr;
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }

        String trimmed = ip.trim();
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            trimmed = trimmed.substring(1, trimmed.indexOf(']'));
        }

        return IP_VALIDATOR.isValid(trimmed) ? trimmed : null;
    }

    private static boolean isTrustedProxy(String ip, List<String> trustedProxyCidrs) {
        if (ip == null || !IP_VALIDATOR.isValid(ip)) {
            return false;
        }

        List<String> trustedCidrs = trustedProxyCidrs == null || trustedProxyCidrs.isEmpty()
                ? LOOPBACK_TRUSTED_CIDRS
                : trustedProxyCidrs;

        for (String trustedCidr : trustedCidrs) {
            if (cidrContains(trustedCidr, ip)) {
                return true;
            }
        }

        return false;
    }

    private static boolean cidrContains(String cidr, String ip) {
        if (cidr == null || cidr.isBlank()) {
            return false;
        }

        try {
            String[] parts = cidr.trim().split("/", 2);
            String networkIp = normalizeIp(parts[0]);
            String targetIp = normalizeIp(ip);
            if (networkIp == null || targetIp == null) {
                return false;
            }

            InetAddress networkAddress = InetAddress.getByName(networkIp);
            InetAddress targetAddress = InetAddress.getByName(targetIp);
            byte[] networkBytes = networkAddress.getAddress();
            byte[] targetBytes = targetAddress.getAddress();
            if (networkBytes.length != targetBytes.length) {
                return false;
            }

            int maxPrefixLength = networkBytes.length * Byte.SIZE;
            int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : maxPrefixLength;
            if (prefixLength < 0 || prefixLength > maxPrefixLength) {
                return false;
            }

            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;

            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != targetBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xff << (Byte.SIZE - remainingBits);
            return (networkBytes[fullBytes] & mask) == (targetBytes[fullBytes] & mask);
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }
}
