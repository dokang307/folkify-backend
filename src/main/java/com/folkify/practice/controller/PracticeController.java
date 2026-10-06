package com.folkify.practice.controller;

import com.folkify.auth.entity.User;
import com.folkify.common.response.ApiResponse;
import com.folkify.practice.dto.PerformanceResponse;
import com.folkify.practice.dto.PerformanceSummaryResponse;
import com.folkify.practice.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/performances")
@Tag(name = "Performances", description = "AI chấm phần trình diễn tác phẩm")
public class PracticeController {

    private final PerformanceService performanceService;

    public PracticeController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Gửi bản ghi một đoạn hoặc cả tác phẩm để AI chấm điểm")
    public ResponseEntity<ApiResponse<PerformanceResponse>> submit(
            @RequestParam("file") MultipartFile file,
            @RequestParam("songId") UUID songId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(performanceService.submit(user, songId, file)));
    }

    @GetMapping
    @Operation(summary = "Lịch sử chấm điểm (giới hạn theo gói)")
    public ResponseEntity<ApiResponse<Page<PerformanceSummaryResponse>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(performanceService.history(user, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một lần chấm của chính mình")
    public ResponseEntity<ApiResponse<PerformanceResponse>> get(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(performanceService.get(user, id)));
    }
}
