package com.convertlab.convertlab_backend.service_web.controllers;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.authentication.LoginService;
import com.convertlab.convertlab_backend.authentication.SignupService;
import com.convertlab.convertlab_backend.service_email.OtpVerificationService;
import com.convertlab.convertlab_backend.service_web.controllers.dto.LoginRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.SignupRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.VerifyOtpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;
    private final OtpVerificationService otpVerificationService;
    private final LoginService loginService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody SignupRequest request) {

        signupService.signup(request);

        return ResponseEntity.ok(ApiResponse.success("Signup successful. Please verify OTP sent to your email."));

    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestBody VerifyOtpRequest request) {

        otpVerificationService.verifyOtp(request);

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {

        loginService.login(request);

        return ResponseEntity.ok(ApiResponse.success("Login successful."));
    }
}
