package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.api.enums.AuthProviders;
import com.convertlab.convertlab_backend.api.enums.UserRole;
import com.convertlab.convertlab_backend.entity.AuthProvider;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.LoginException;
import com.convertlab.convertlab_backend.repository.AuthProviderRepository;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.convertlab.convertlab_backend.security_util.CookieUtil;
import com.convertlab.convertlab_backend.security_util.JwtUtil;
import com.convertlab.convertlab_backend.security_util.PasswordUtil;
import com.convertlab.convertlab_backend.service_web.controllers.dto.AuthTokenResponse;
import com.convertlab.convertlab_backend.service_web.controllers.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    private final UserRoleService userRoleService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.cookie.same-site:Strict}")
    private String sameSite;


    /**
     * Authenticates the user, then:
     *  - Creates a refresh token, saves it to DB, sets it in an HttpOnly cookie
     *  - Returns an access token in the response body
     */
    @Transactional
    public AuthTokenResponse login(LoginRequest request, HttpServletResponse response) {

        boolean userExistAndVerified =
                userRepository.existsByEmailAndEmailVerifiedTrue(request.email());

        if (!userExistAndVerified) {
            throw new LoginException(
                    "User not found or not verified",
                    "USER_NOT_FOUND_OR_NOT_VERIFIED",
                    HttpStatus.NOT_FOUND
            );
        }

        Optional<AuthProvider> authProvider = authProviderRepository.findByProviderAndProviderUserId(AuthProviders.LOCAL, request.email());

        if (authProvider.isEmpty()) {
            throw new LoginException(
                    "Local authentication not configured for this user",
                    "LOCAL_AUTH_NOT_CONFIGURED",
                    HttpStatus.UNAUTHORIZED
            );
        }

        boolean isAuthenticated =
                passwordUtil.matches(request.password(), authProvider.get().getPasswordHash());

        if (!isAuthenticated) {
            throw new LoginException(
                    "Invalid email or password",
                    "INVALID_EMAIL_PASSWORD",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return issueTokens(request.email(), response);
    }

    // Called by LoginService AND the token-refresh flow
    public AuthTokenResponse issueTokens(String email, HttpServletResponse response) {
        // Refresh token — long-lived, goes in HttpOnly cookie
        String refreshToken = refreshTokenService.createAndSave(email);
        cookieUtil.setRefreshTokenCookie(
                response,
                refreshToken,
                jwtUtil.getRefreshTokenExpirySeconds(),
                secureCookie,
                sameSite
        );

        return issueAccessTokenResponse(email);
    }

    public AuthTokenResponse issueAccessTokenResponse(String email) {
        User user = userRoleService.applyConfiguredRole(email);
        UserRole role = user.getRole() == null ? UserRole.USER : user.getRole();
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), role);

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresInSeconds(jwtUtil.getAccessTokenExpirySeconds())
                .email(user.getEmail())
                .role(role.name())
                .build();
    }
}
