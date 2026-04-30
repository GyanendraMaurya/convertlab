package com.convertlab.convertlab_backend.service_email.impl;

import com.convertlab.convertlab_backend.service_email.EmailSender;
import com.convertlab.convertlab_backend.service_email.ContactInquiryEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Primary
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtp(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("ConvertLab – Your OTP Code");
        message.setText("""
                ConvertLab
                
                Your OTP code is: %s

                This code is valid for 5 minutes.
                If you did not request this, please ignore this email.
                
                — ConvertLab Team
                """.formatted(otp));

        mailSender.send(message);
    }

    @Override
    public void sendContactInquiry(String toEmail, ContactInquiryEmail inquiry) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("New ConvertLab contact inquiry from " + inquiry.fullName());
        message.setText("""
                New ConvertLab contact inquiry

                Name: %s
                Email: %s
                Phone: %s
                Project type: %s
                Budget range: %s
                Submitted at: %s
                Inquiry id: %s

                Message:
                %s
                """.formatted(
                inquiry.fullName(),
                displayValue(inquiry.email()),
                displayValue(inquiry.phone()),
                displayValue(inquiry.inquiryType()),
                displayValue(inquiry.budgetRange()),
                inquiry.submittedAt(),
                inquiry.inquiryId(),
                inquiry.message()
        ));

        mailSender.send(message);
    }

    private static String displayValue(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
