package com.convertlab.convertlab_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "page_visit")
@AllArgsConstructor
@Getter
public class PageVisit implements Persistable<UUID> {

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

    // Geographic data
    @Column(name = "ip_hash")
    private String ipHash;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "country_code")
    private String countryCode;

    protected PageVisit() {
        // JPA only
    }


    @Override
    public boolean isNew() {
        return true;
    }
}

