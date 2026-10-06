package com.folkify.quiz.dto;

import com.folkify.quiz.entity.QuestionType;
import com.folkify.quiz.entity.QuizOption;
import com.folkify.quiz.entity.QuizQuestion;

import java.util.List;
import java.util.UUID;

/** Đề quiz cho learner — cố ý KHÔNG chứa đáp án đúng. */
public record QuizResponse(UUID lessonId, int passPercent, List<QuestionDto> questions) {

    public record QuestionDto(UUID id, String question, QuestionType type, List<OptionDto> options) {
        public static QuestionDto from(QuizQuestion q) {
            return new QuestionDto(q.getId(), q.getQuestion(), q.getType(),
                    q.getOptions().stream().map(OptionDto::from).toList());
        }
    }

    public record OptionDto(UUID id, String text) {
        public static OptionDto from(QuizOption o) {
            return new OptionDto(o.getId(), o.getText());
        }
    }
}
