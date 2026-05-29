package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.BroadcastMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, UUID> {

    List<BroadcastMessage> findAllByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);

    List<BroadcastMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
