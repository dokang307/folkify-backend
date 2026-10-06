package com.folkify.instrument.controller;

import com.folkify.auth.entity.User;
import com.folkify.common.response.ApiResponse;
import com.folkify.instrument.dto.*;
import com.folkify.instrument.service.InstrumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@Tag(name = "Instruments", description = "API nhạc cụ dân tộc Việt Nam")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    @Operation(summary = "Danh sách tất cả nhạc cụ")
    public ResponseEntity<ApiResponse<List<InstrumentSummaryResponse>>> getAllInstruments() {
        return ResponseEntity.ok(ApiResponse.success(instrumentService.getAllInstruments()));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Chi tiết nhạc cụ")
    public ResponseEntity<ApiResponse<InstrumentDetailResponse>> getInstrument(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(instrumentService.getInstrumentBySlug(slug, user)));
    }

    @GetMapping("/{slug}/lessons")
    @Operation(summary = "Danh sách bài học của nhạc cụ")
    public ResponseEntity<ApiResponse<List<LessonSummaryResponse>>> getLessons(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(instrumentService.getLessonsByInstrument(slug, user)));
    }

    @GetMapping("/{slug}/lessons/{lessonSlug}")
    @Operation(summary = "Chi tiết bài học")
    public ResponseEntity<ApiResponse<LessonDetailResponse>> getLessonDetail(
            @PathVariable String slug,
            @PathVariable String lessonSlug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(instrumentService.getLessonDetail(slug, lessonSlug, user)));
    }

    @GetMapping("/{slug}/songs")
    @Operation(summary = "Danh sách tác phẩm của nhạc cụ (kèm trạng thái chấm điểm AI theo gói)")
    public ResponseEntity<ApiResponse<List<SongResponse>>> getSongs(
            @PathVariable String slug,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(instrumentService.getSongsByInstrument(slug, user)));
    }
}
