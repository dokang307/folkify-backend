package com.folkify.quiz.dto;

import com.folkify.progress.dto.CompleteLessonResponse;

import java.util.List;
import java.util.UUID;

/** Kết quả chấm quiz. {@code completion} chỉ có khi lần này làm lesson chuyển sang hoàn thành. */
public record QuizResultResponse(
        int scorePercent,
        int correctCount,
        int totalCount,
        boolean passed,
        List<QuestionResult> results,
        CompleteLessonResponse completion
) {
    public record QuestionResult(UUID questionId, boolean correct, List<UUID> correctOptionIds, String explanation) {}
}
