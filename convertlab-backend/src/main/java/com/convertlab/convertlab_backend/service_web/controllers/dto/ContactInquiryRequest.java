package com.convertlab.convertlab_backend.service_web.controllers.dto;

public record ContactInquiryRequest(
        String fullName,
        String email,
        String phone,
        String message,
        String inquiryType,
        String budgetRange
) {
}
