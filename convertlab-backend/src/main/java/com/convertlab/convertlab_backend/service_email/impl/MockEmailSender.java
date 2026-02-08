package com.convertlab.convertlab_backend.service_email.impl;

import com.convertlab.convertlab_backend.service_email.EmailSender;
import org.springframework.stereotype.Service;

@Service
public class MockEmailSender implements EmailSender {

    @Override
    public void sendOtp(String email, String otp) {
        System.out.println("==== MOCK EMAIL ====");
        System.out.println("To: " + email);
        System.out.println("OTP: " + otp);
        System.out.println("====================");
    }
}
