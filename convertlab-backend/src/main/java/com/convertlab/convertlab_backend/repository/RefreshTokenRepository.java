package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByIdAndRevokedFalse(UUID id);

    /** Revoke all active tokens for a user — call this on password change / suspicious activity */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.email = :email AND rt.revoked = false")
    int revokeAllByEmail(String email);

    /** Periodic cleanup — delete tokens that have already expired */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpired(Instant now);
}