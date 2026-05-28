package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.api.enums.UserRole;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserRoleService implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.security.super-admin-emails:}")
    private String superAdminEmails;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        configuredSuperAdminEmails().forEach(email ->
                userRepository.findByEmailIgnoreCase(email).ifPresent(this::promoteToSuperAdminIfNeeded)
        );
    }

    @Transactional
    public User applyConfiguredRole(User user) {
        if (isConfiguredSuperAdmin(user.getEmail())) {
            return promoteToSuperAdminIfNeeded(user);
        }
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
            return userRepository.save(user);
        }
        return user;
    }

    @Transactional
    public User applyConfiguredRole(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        return applyConfiguredRole(user);
    }

    private User promoteToSuperAdminIfNeeded(User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return user;
        }

        user.setRole(UserRole.SUPER_ADMIN);
        User savedUser = userRepository.save(user);
        log.info("Promoted configured super admin user: {}", user.getEmail());
        return savedUser;
    }

    private boolean isConfiguredSuperAdmin(String email) {
        if (email == null) {
            return false;
        }
        return configuredSuperAdminEmails().contains(email.trim().toLowerCase());
    }

    private Set<String> configuredSuperAdminEmails() {
        if (superAdminEmails == null || superAdminEmails.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(superAdminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
