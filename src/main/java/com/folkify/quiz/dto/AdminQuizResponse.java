package com.folkify.quiz.dto;

import com.folkify.quiz.entity.QuestionType;
import com.folkify.quiz.entity.QuizQuestion;

import java.util.List;
import java.util.UUID;

/** Quiz đầy đủ (kèm đáp án) cho admin. */
public record AdminQuizResponse(UUID lessonId, List<Question> questions) {

    public record Question(UUID id, String question, QuestionType type, String explanation, List<Option> options) {
        public static Question from(QuizQuestion q) {
            return new Question(q.getId(), q.getQuestion(), q.getType(), q.getExplanation(),
                    q.getOptions().stream().map(o -> new Option(o.getId(), o.getText(), o.isCorrect())).toList());
        }
    }

    public record Option(UUID id, String text, boolean correct) {}
}
