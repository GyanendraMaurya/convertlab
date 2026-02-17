package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Returned in the JSON body after login / token refresh.
 * The refresh token is NOT here — it lives in an HttpOnly cookie.
 */
@Getter
@Builder
public class AuthTokenResponse {
    private String accessToken;
    private long   accessTokenExpiresInSeconds;
    private String email;
}