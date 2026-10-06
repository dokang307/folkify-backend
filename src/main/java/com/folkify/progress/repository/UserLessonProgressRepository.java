package com.folkify.progress.repository;

import com.folkify.progress.entity.UserLessonProgress;
import com.folkify.progress.entity.UserLessonProgressId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, UserLessonProgressId> {

    boolean existsByIdUserIdAndIdLessonId(UUID userId, UUID lessonId);

    @Query("SELECT p.id.lessonId FROM UserLessonProgress p WHERE p.id.userId = :userId AND p.lesson.instrument.slug = :slug")
    java.util.Set<UUID> findCompletedLessonIds(@Param("userId") UUID userId, @Param("slug") String instrumentSlug);

    @Query("SELECT COUNT(p) FROM UserLessonProgress p WHERE p.id.userId = :userId")
    long countCompletedByUser(@Param("userId") UUID userId);

    @Query("""
        SELECT COUNT(p) FROM UserLessonProgress p
        JOIN p.lesson l
        WHERE p.id.userId = :userId AND l.instrument.id = :instrumentId
    """)
    long countCompletedByUserAndInstrument(@Param("userId") UUID userId, @Param("instrumentId") UUID instrumentId);

    @Query("""
        SELECT COALESCE(SUM(l.xp), 0) FROM UserLessonProgress p
        JOIN p.lesson l
        WHERE p.id.userId = :userId
    """)
    long sumXpByUser(@Param("userId") UUID userId);
}
