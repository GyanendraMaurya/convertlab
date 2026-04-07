package com.convertlab.convertlab_backend.entity;

import com.convertlab.convertlab_backend.api.enums.AuthProviders;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "auth_providers")
public class AuthProvider {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private AuthProviders provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

}