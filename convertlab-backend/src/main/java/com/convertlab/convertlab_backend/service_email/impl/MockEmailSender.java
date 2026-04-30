package com.convertlab.convertlab_backend.service_email.impl;

import com.convertlab.convertlab_backend.service_email.ContactInquiryEmail;
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

    @Override
    public void sendContactInquiry(String toEmail, ContactInquiryEmail inquiry) {
        System.out.println("==== MOCK CONTACT EMAIL ====");
        System.out.println("To: " + toEmail);
        System.out.println("Inquiry id: " + inquiry.inquiryId());
        System.out.println("Name: " + inquiry.fullName());
        System.out.println("Email: " + inquiry.email());
        System.out.println("Phone: " + inquiry.phone());
        System.out.println("Project type: " + inquiry.inquiryType());
        System.out.println("Budget range: " + inquiry.budgetRange());
        System.out.println("Submitted at: " + inquiry.submittedAt());
        System.out.println("Message: " + inquiry.message());
        System.out.println("============================");
    }
}
