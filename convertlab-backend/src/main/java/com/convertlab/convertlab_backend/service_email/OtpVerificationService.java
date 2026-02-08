package com.convertlab.convertlab_backend.service_email;

import com.convertlab.convertlab_backend.entity.EmailOtp;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.repository.EmailOtpRepository;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.service_web.controllers.dto.VerifyOtpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpVerificationService {

    private final EmailOtpRepository emailOtpRepository;
    private final UserRepository userRepository;

    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {

        EmailOtp emailOtp = emailOtpRepository
                .findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new IllegalStateException("OTP not found or already used"));

        if (emailOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("OTP expired");
        }

        if (!emailOtp.getOtpHash().equals("HASHED_" + request.otp())) {
            throw new IllegalStateException("Invalid OTP");
        }

        // mark OTP consumed
        emailOtp.setConsumed(true);
        emailOtpRepository.save(emailOtp);

        // verify user email
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        user.setEmailVerified(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}

