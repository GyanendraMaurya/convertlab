package com.convertlab.convertlab_backend.security_util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Helper to set / clear the HttpOnly refresh-token cookie.
 * <p>
 * Security attributes used:
 *  - HttpOnly     → JS cannot read it
 *  - Secure       → only sent over HTTPS (set to false in local dev via config)
 *  - SameSite=Strict → cookie is only sent on same-origin navigations,
 *                     blocks CSRF from third-party sites completely
 *  - Path=/api/auth → cookie is only sent to the auth endpoints, not the
 *                     whole API, which reduces the attack surface
 */
@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    /**
     * Writes the refresh token cookie to the response.
     *
     * @param secure set true in production (HTTPS), false in local dev
     */
    public void setRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken,
            long maxAgeSeconds,
            boolean secure,
            String sameSite
    ) {
        // Jakarta Servlet 6 Cookie doesn't expose SameSite directly,
        // so we write the Set-Cookie header manually for full control.
        String cookie = buildSetCookieHeader(REFRESH_TOKEN_COOKIE, refreshToken, maxAgeSeconds, secure, sameSite);
        response.addHeader("Set-Cookie", cookie);
    }

    /**
     * Overwrites the refresh token cookie with an expired, empty value — effectively deleting it.
     */
    public void clearRefreshTokenCookie(HttpServletResponse response, boolean secure, String sameSite) {
        String cookie = buildSetCookieHeader(REFRESH_TOKEN_COOKIE, "", 0, secure, sameSite);
        response.addHeader("Set-Cookie", cookie);
    }

    /**
     * Reads the refresh token from incoming cookies.
     */
    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private String buildSetCookieHeader(
            String name,
            String value,
            long maxAgeSeconds,
            boolean secure,
            String sameSite
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value).append("; ");
        sb.append("Max-Age=").append(maxAgeSeconds).append("; ");
        sb.append("Path=").append(COOKIE_PATH).append("; ");
        if (secure) {
            sb.append("Secure; ");
        }
        sb.append("HttpOnly; ");
        sb.append("SameSite=").append(sameSite);
        return sb.toString();
    }
}