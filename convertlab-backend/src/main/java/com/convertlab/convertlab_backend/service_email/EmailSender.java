package com.convertlab.convertlab_backend.service_email;

public interface EmailSender {
    void sendOtp(String email, String otp);

    void sendContactInquiry(String toEmail, ContactInquiryEmail inquiry);
}
