package com.folkify.practice.repository;

import com.folkify.practice.entity.PerformanceAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PerformanceAttemptRepository extends JpaRepository<PerformanceAttempt, UUID> {

    @EntityGraph(attributePaths = {"song", "instrument"})
    Page<PerformanceAttempt> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime after, Pageable pageable);

    @EntityGraph(attributePaths = {"song", "instrument"})
    Optional<PerformanceAttempt> findByIdAndUserId(UUID id, UUID userId);
}
