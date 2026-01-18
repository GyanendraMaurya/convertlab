package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "page_visit")
@AllArgsConstructor
@Getter
public class PageVisit {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "path")
    private String path;

    @Column(name = "entry")
    private Boolean entry;

    @Column(name = "visited_at")
    private Instant visitedAt;

    protected PageVisit() {
        // JPA only
    }


}

