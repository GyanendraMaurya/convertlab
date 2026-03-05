package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_ai_usage")
@Getter
@Setter
public class UserAiUsage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Column(name = "ingest_count")
    private int ingestCount = 0;

    @Column(name = "query_count")
    private int queryCount = 0;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected UserAiUsage() {}

    public UserAiUsage(UUID id, String email, LocalDate usageDate) {
        this.id = id;
        this.email = email;
        this.usageDate = usageDate;
        this.updatedAt = Instant.now();
    }
}
