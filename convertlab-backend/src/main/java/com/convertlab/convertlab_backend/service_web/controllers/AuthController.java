package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.authentication.GoogleOAuthService;
import com.convertlab.convertlab_backend.authentication.LoginService;
import com.convertlab.convertlab_backend.authentication.RefreshTokenService;
import com.convertlab.convertlab_backend.authentication.SignupService;
import com.convertlab.convertlab_backend.exception.LoginException;
import com.convertlab.convertlab_backend.security_util.CookieUtil;
import com.convertlab.convertlab_backend.security_util.JwtUtil;
import com.convertlab.convertlab_backend.service_email.OtpVerificationService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;
    private final OtpVerificationService otpVerificationService;
    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private final GoogleOAuthService googleOAuthService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Strict}")
    private String sameSite;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody SignupRequest request) {
        signupService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Signup successful. Please verify OTP sent to your email."
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        otpVerificationService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully."));
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    /**
     * Returns:
     *  - JSON body: { accessToken, accessTokenExpiresInSeconds, email }
     *  - Cookie: refresh_token (HttpOnly, Secure, SameSite=Strict)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokens = loginService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * Client calls this when the access token expires.
     * Reads the refresh token from the HttpOnly cookie, rotates it, and returns a new access token.
     *
     * Refresh token rotation: old token is revoked in DB, a new one is issued and set in the cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String rawRefreshToken = cookieUtil.extractRefreshToken(request)
                .orElseThrow(() -> new LoginException(
                        "No refresh token found",
                        "MISSING_REFRESH_TOKEN",
                        HttpStatus.UNAUTHORIZED
                ));

        // rotate() validates JWT, checks DB, detects reuse, and returns new refresh token + email
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);

        // Set new refresh token cookie
        cookieUtil.setRefreshTokenCookie(
                response,
                result.newRefreshToken(),
                jwtUtil.getRefreshTokenExpirySeconds(),
                secureCookie,
                sameSite
        );

        // Issue new access token
        String accessToken = jwtUtil.generateAccessToken(result.email());

        AuthTokenResponse tokens = AuthTokenResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresInSeconds(jwtUtil.getAccessTokenExpirySeconds())
                .email(result.email())
                .build();

        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    // ─── Logout ──────────────────────────────────────────────────────────────────

    /**
     * Revokes ALL refresh tokens for the user and clears the cookie.
     * The access token will naturally expire in ≤5 minutes — you can also maintain a
     * short-lived blocklist if you need instant access token invalidation.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        cookieUtil.extractRefreshToken(request).ifPresent(rawToken -> {
            try {
                var claims = jwtUtil.validateRefreshTokenAndGetClaims(rawToken);
                refreshTokenService.revokeAll(claims.getSubject());
            } catch (Exception e) {
                // Token may already be invalid — still clear the cookie
            }
        });

        cookieUtil.clearRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> googleLogin(
            @RequestBody GoogleLoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokens = googleOAuthService.loginWithGoogle(request.idToken(), response);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }
}