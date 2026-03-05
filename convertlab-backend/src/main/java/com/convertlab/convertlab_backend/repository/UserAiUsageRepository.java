package com.convertlab.convertlab_backend.repository;

import com.convertlab.convertlab_backend.entity.UserAiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface UserAiUsageRepository extends JpaRepository<UserAiUsage, Long> {

    Optional<UserAiUsage> findByEmailAndUsageDate(String email, LocalDate date);

    @Modifying
    @Query("""
            UPDATE UserAiUsage u
            SET u.ingestCount = u.ingestCount + 1, u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.email = :email AND u.usageDate = :date
            """)
    int incrementIngest(@Param("email") String email, @Param("date") LocalDate date);

    @Modifying
    @Query("""
            UPDATE UserAiUsage u
            SET u.queryCount = u.queryCount + 1, u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.email = :email AND u.usageDate = :date
            """)
    int incrementQuery(@Param("email") String email, @Param("date") LocalDate date);
}
