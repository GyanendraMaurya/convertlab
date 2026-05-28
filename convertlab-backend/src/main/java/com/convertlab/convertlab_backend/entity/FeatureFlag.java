package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "feature_flags")
@Getter
public class FeatureFlag {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "expose_to_frontend", nullable = false)
    private boolean exposeToFrontend;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FeatureFlag() {
        // JPA only
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
