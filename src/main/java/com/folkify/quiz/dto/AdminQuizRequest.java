package com.folkify.quiz.dto;

import com.folkify.quiz.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Thay toàn bộ quiz của một bài học. Thứ tự trong list = thứ tự hiển thị. */
public record AdminQuizRequest(@NotNull @Valid List<AdminQuestion> questions) {

    public record AdminQuestion(
            @NotBlank String question,
            @NotNull QuestionType type,
            String explanation,
            @NotNull @Size(min = 2) @Valid List<AdminOption> options
    ) {}

    public record AdminOption(@NotBlank String text, boolean correct) {}
}
