package com.folkify.quiz.repository;

import com.folkify.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    @Query("SELECT DISTINCT q FROM QuizQuestion q LEFT JOIN FETCH q.options "
            + "WHERE q.lesson.id = :lessonId ORDER BY q.orderIndex ASC")
    List<QuizQuestion> findWithOptionsByLessonId(@Param("lessonId") UUID lessonId);

    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.lesson.id = :lessonId")
    void deleteByLessonId(@Param("lessonId") UUID lessonId);
}
