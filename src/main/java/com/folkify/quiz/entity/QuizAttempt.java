package com.folkify.quiz.entity;

import com.folkify.auth.entity.User;
import com.folkify.infrastructure.persistence.BaseEntity;
import com.folkify.instrument.entity.Lesson;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    /** questionId → danh sách optionId user đã chọn. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<UUID, List<UUID>> answers;

    protected QuizAttempt() {}

    public QuizAttempt(User user, Lesson lesson, int scorePercent, int correctCount, int totalCount,
                       Map<UUID, List<UUID>> answers) {
        this.user = user;
        this.lesson = lesson;
        this.scorePercent = scorePercent;
        this.correctCount = correctCount;
        this.totalCount = totalCount;
        this.answers = answers;
    }

    public int getScorePercent() { return scorePercent; }
    public int getCorrectCount() { return correctCount; }
    public int getTotalCount() { return totalCount; }
    public Map<UUID, List<UUID>> getAnswers() { return answers; }
}
