package com.folkify.quiz.controller;

import com.folkify.auth.entity.User;
import com.folkify.common.response.ApiResponse;
import com.folkify.quiz.dto.QuizResponse;
import com.folkify.quiz.dto.QuizResultResponse;
import com.folkify.quiz.dto.SubmitQuizRequest;
import com.folkify.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instruments/{slug}/lessons/{lessonSlug}/quiz")
@Tag(name = "Quiz", description = "Câu hỏi ôn tập lý thuyết theo bài học")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    @Operation(summary = "Lấy đề quiz của bài học (không kèm đáp án)")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable String slug,
            @PathVariable String lessonSlug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getQuiz(slug, lessonSlug, user)));
    }

    @PostMapping("/attempts")
    @Operation(summary = "Nộp bài quiz, chấm điểm phía server")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submit(
            @PathVariable String slug,
            @PathVariable String lessonSlug,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(ApiResponse.success(quizService.submit(slug, lessonSlug, user, request)));
    }
}
