package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {
    List<AuthProvider> findByUserId(UUID userId);
    Optional<AuthProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
    boolean existsByUserIdAndProvider(UUID userId, String provider);
}

