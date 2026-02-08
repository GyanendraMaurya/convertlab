package com.convertlab.convertlab_backend.service_web.controllers.dto;

public record VerifyOtpRequest(
        String email,
        String otp
) {}

