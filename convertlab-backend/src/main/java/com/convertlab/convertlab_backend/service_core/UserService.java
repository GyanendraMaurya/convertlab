package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.authentication.RefreshTokenService;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.NotFoundException;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.security_util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    @Transactional
    public void deletePrincipalUser(String email, HttpServletResponse response) {
        if (email == null || email.equals("anonymousUser")) {
            throw new AuthenticationException("Authentication required") {};
        }
        // check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!email.equals(user.getEmail())) {
            throw new AccessDeniedException("Unauthorized to delete user");
        }

        userRepository.deleteById(user.getId());
        refreshTokenService.revokeAll(user.getEmail());
        cookieUtil.clearRefreshTokenCookie(response);

    }
}
