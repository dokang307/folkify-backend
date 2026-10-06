package com.folkify.quiz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** questionId → các optionId đã chọn. Câu không trả lời được tính sai. */
public record SubmitQuizRequest(@NotNull Map<UUID, List<UUID>> answers) {}
