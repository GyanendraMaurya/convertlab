package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.entity.EmailOtp;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.repository.EmailOtpRepository;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.service_email.EmailSender;
import com.convertlab.convertlab_backend.service_web.controllers.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailSender emailSender;

    @Transactional
    public void signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already registered: " + request.email());
        }

        User user = new User(
                UUID.randomUUID(),
                request.email(),
                "HASHED_" + request.password(),
                false,
                Instant.now(),
                Instant.now()
        );
        userRepository.save(user);

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        EmailOtp emailOtp = new EmailOtp(
                UUID.randomUUID(),
                request.email(),
                "HASHED_" + otp,
                Instant.now().plusSeconds(5 * 60),
                false,
                Instant.now()
        );

        emailOtpRepository.save(emailOtp);
        emailSender.sendOtp(request.email(), otp);
    }
}

