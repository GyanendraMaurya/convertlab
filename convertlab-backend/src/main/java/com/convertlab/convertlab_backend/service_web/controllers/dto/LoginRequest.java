package com.convertlab.convertlab_backend.service_web.controllers.dto;

public record LoginRequest(
        String email,
        String password
) {}
