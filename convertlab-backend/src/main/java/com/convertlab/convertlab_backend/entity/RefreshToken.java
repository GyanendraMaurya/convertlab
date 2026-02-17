package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted refresh token.
 * <p>
 * Storing it in the DB lets you:
 *  - Revoke a single token (logout from one device)
 *  - Revoke ALL tokens for a user (change password, suspicious activity)
 *  - Detect refresh-token reuse (rotation theft detection)
 */
@Entity
@Table(name = "refresh_tokens")
@AllArgsConstructor
@Getter
@Setter
public class RefreshToken {

    @Id
    private UUID id;           // equals the JWT "jti" claim

    @Column(nullable = false)
    private String email;

    /**
     * Null until the token is rotated (consumed and a new one issued).
     * If you receive a request using a revoked token, someone reused a stolen token →
     * revoke ALL tokens for that user.
     */
    @Column(name = "revoked")
    private boolean revoked;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
        // JPA only
    }
}
