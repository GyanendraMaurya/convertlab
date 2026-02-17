package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.LoginException;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.security_util.PasswordUtil;
import com.convertlab.convertlab_backend.service_web.controllers.dto.LoginRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;

    @Transactional
    public void login(LoginRequest request) {
        boolean userExistAndVerified = userRepository.existsByEmailAndEmailVerifiedTrue(request.email());
        if (!userExistAndVerified) {
            throw new LoginException("User not found or not verified", "USER_NOT_FOUND_OR_NOT_VERIFIED", HttpStatus.NOT_FOUND);
        }
        Optional<User> user = userRepository.findByEmail(request.email());
        if (user.isEmpty()) {
            return;
        }
        boolean isAuthenticated = passwordUtil.matches(request.password(), user.get().getPasswordHash());
        if (!isAuthenticated) {
            throw new LoginException("Invalid email or password", "INVALID_EMAIL_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

    }
}
