package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.entity.EmailOtp;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.SignUpValidationException;
import com.convertlab.convertlab_backend.repository.EmailOtpRepository;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.security_util.PasswordUtil;
import com.convertlab.convertlab_backend.service_email.EmailSender;
import com.convertlab.convertlab_backend.service_util.EmailUtil;
import com.convertlab.convertlab_backend.service_util.OtpUtil;
import com.convertlab.convertlab_backend.service_web.controllers.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailSender emailSender;
    private final PasswordUtil passwordUtil;

    @Transactional
    public void signup(SignupRequest request) {

        validateSignupRequest(request);

        if (userRepository.existsByEmailAndEmailVerifiedTrue(request.email())) {
            throw new IllegalStateException("Email already registered: " + request.email());
        }

        Optional<User> existingUser = userRepository.findByEmail(request.email());
        User user;
        if (existingUser.isPresent()) {
            // user exists but not verified → reuse
            user = existingUser.get();
            user.setPasswordHash(passwordUtil.hash(request.password()));
            user.setUpdatedAt(Instant.now());
        } else {
            // New user
            user = new User(
                    UUID.randomUUID(),
                    request.email(),
                    passwordUtil.hash(request.password()),
                    false,
                    Instant.now(),
                    Instant.now()
            );
        }

        userRepository.save(user);

        String otp = OtpUtil.generateOtp();

        EmailOtp emailOtp = new EmailOtp(
                UUID.randomUUID(),
                request.email(),
                OtpUtil.hash(otp),
                Instant.now().plusSeconds(5 * 60),
                false,
                Instant.now()
        );

        emailOtpRepository.save(emailOtp);
        emailSender.sendOtp(request.email(), otp);
    }

    private void validateSignupRequest(SignupRequest request) {
        if (request.email() == null || request.password() == null) {
            throw new SignUpValidationException("Invalid email or password", "INVALID_EMAIL_PASSWORD");
        }

        if (!EmailUtil.isValid(request.email())) {
            throw new SignUpValidationException("Invalid email", "INVALID_EMAIL");
        }

    }

}

