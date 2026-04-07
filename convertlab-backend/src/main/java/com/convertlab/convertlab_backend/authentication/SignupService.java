package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.api.enums.AuthProviders;
import com.convertlab.convertlab_backend.entity.AuthProvider;
import com.convertlab.convertlab_backend.entity.EmailOtp;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.SignUpValidationException;
import com.convertlab.convertlab_backend.repository.AuthProviderRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
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
            user.setUpdatedAt(Instant.now());
        } else {
            // New user
            user = new User(
                    UUID.randomUUID(),
                    request.email(),
                    false,
                    Instant.now(),
                    Instant.now()
            );
        }

        User savedUser = userRepository.save(user);

        // Create AuthProvider entry for local provider with password
        AuthProvider authProvider = new AuthProvider();
        authProvider.setId(UUID.randomUUID());
        authProvider.setUser(savedUser);
        authProvider.setProvider(AuthProviders.LOCAL);
        authProvider.setProviderUserId(request.email());
        authProvider.setPasswordHash(passwordUtil.hash(request.password()));
        authProvider.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        authProviderRepository.save(authProvider);

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
        if (request.email() == null || request.password() == null || request.email().trim().isEmpty() || request.password().trim().isEmpty()) {
            throw new SignUpValidationException("Invalid email or password", "INVALID_EMAIL_PASSWORD");
        }

        if (!EmailUtil.isValid(request.email())) {
            throw new SignUpValidationException("Invalid email", "INVALID_EMAIL");
        }

        if(request.password().trim().length() < 5){
            throw new SignUpValidationException("Password should be minimum 5 characters", "PASSWORD_TOO_SHORT");
        }

    }

}

