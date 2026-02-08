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
@Table(name = "users")
@AllArgsConstructor
@Getter
@Setter
public class User {

    @Id
    private UUID id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public User() {
    }
}

