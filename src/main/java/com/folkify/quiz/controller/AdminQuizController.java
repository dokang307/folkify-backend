package com.folkify.quiz.controller;

import com.folkify.common.response.ApiResponse;
import com.folkify.quiz.dto.AdminQuizRequest;
import com.folkify.quiz.dto.AdminQuizResponse;
import com.folkify.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/lessons/{lessonId}/quiz")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Quiz", description = "Quản lý câu hỏi ôn tập")
public class AdminQuizController {

    private final QuizService quizService;

    public AdminQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    @Operation(summary = "Xem quiz của bài học (kèm đáp án)")
    public ResponseEntity<ApiResponse<AdminQuizResponse>> getQuiz(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getAdminQuiz(lessonId)));
    }

    @PutMapping
    @Operation(summary = "Thay toàn bộ quiz của bài học")
    public ResponseEntity<ApiResponse<AdminQuizResponse>> replaceQuiz(
            @PathVariable UUID lessonId,
            @Valid @RequestBody AdminQuizRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quiz thành công", quizService.replaceQuiz(lessonId, request)));
    }
}
