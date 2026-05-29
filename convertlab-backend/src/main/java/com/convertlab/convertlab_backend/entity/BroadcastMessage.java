package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "broadcast_messages")
@Getter
public class BroadcastMessage {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected BroadcastMessage() {
        // JPA only
    }

    public BroadcastMessage(String message, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.message = message;
        this.active = true;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
