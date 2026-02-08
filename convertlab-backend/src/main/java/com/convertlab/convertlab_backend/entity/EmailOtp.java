package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_otp")
@AllArgsConstructor
@Getter
@Setter
public class EmailOtp {

    @Id
    private UUID id;

    private String email;

    @Column(name = "otp_hash")
    private String otpHash;

    @Column(name = "expires_at")
    private Instant expiresAt;

    private boolean consumed;

    @Column(name = "created_at")
    private Instant createdAt;

    protected EmailOtp() {
        // JPA only
    }

}

