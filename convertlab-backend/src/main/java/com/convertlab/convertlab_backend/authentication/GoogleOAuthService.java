package com.convertlab.convertlab_backend.authentication;

import com.convertlab.convertlab_backend.api.enums.AuthProviders;
import com.convertlab.convertlab_backend.entity.AuthProvider;
import com.convertlab.convertlab_backend.entity.User;
import com.convertlab.convertlab_backend.exception.LoginException;
import com.convertlab.convertlab_backend.repository.AuthProviderRepository;
import com.convertlab.convertlab_backend.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final LoginService loginService;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    private static final AuthProviders PROVIDER = AuthProviders.GOOGLE;

    /**
     * Verifies the Google ID token, resolves or creates the user,
     * then issues JWT tokens exactly like the local login flow.
     */
    @Transactional
    public com.convertlab.convertlab_backend.service_web.controllers.dto.AuthTokenResponse
    loginWithGoogle(String idToken, HttpServletResponse response) {

        GoogleIdToken.Payload payload = verifyToken(idToken);

        String email = payload.getEmail();
        String googleSub = payload.getSubject(); // stable unique ID
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        if (!emailVerified) {
            throw new LoginException(
                    "Google account email is not verified",
                    "GOOGLE_EMAIL_NOT_VERIFIED",
                    HttpStatus.UNAUTHORIZED
            );
        }

        log.info("Google OAuth login attempt for email: {}", email);

        // ── Case A: existing Google auth_provider row ─────────────────────
        Optional<AuthProvider> existingProvider =
                authProviderRepository.findByProviderAndProviderUserId(PROVIDER, googleSub);

        if (existingProvider.isPresent()) {
            log.info("Existing Google user login: {}", email);
            return loginService.issueTokens(email, response);
        }

        // ── Case B/C: no Google provider row yet ──────────────────────────
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Case C — brand new user
                    log.info("Creating new user via Google OAuth: {}", email);
                    User newUser = new User(
                            UUID.randomUUID(),
                            email,
                            true,          // Google emails are already verified
                            Instant.now(),
                            Instant.now()
                    );
                    return userRepository.save(newUser);
                });

        // Ensure the user is marked as email-verified (Case B — existing local user)
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }

        // Case B — link Google provider to existing account
        // (also runs for Case C — attaches provider to the new user)
        AuthProvider googleProvider = new AuthProvider();
        googleProvider.setId(UUID.randomUUID());
        googleProvider.setUser(user);
        googleProvider.setProvider(PROVIDER);
        googleProvider.setProviderUserId(googleSub);
        googleProvider.setPasswordHash(null); // no password for OAuth
        googleProvider.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        authProviderRepository.save(googleProvider);

        log.info("Google OAuth provider linked for: {}", email);
        return loginService.issueTokens(email, response);
    }

    // ── Token verification ────────────────────────────────────────────────────

    private GoogleIdToken.Payload verifyToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);

            if (token == null) {
                throw new LoginException(
                        "Invalid Google ID token",
                        "INVALID_GOOGLE_TOKEN",
                        HttpStatus.UNAUTHORIZED
                );
            }

            return token.getPayload();

        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new LoginException(
                    "Failed to verify Google token",
                    "GOOGLE_TOKEN_VERIFICATION_FAILED",
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}