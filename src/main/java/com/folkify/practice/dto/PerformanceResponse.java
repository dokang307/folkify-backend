package com.folkify.practice.dto;

import com.folkify.practice.entity.PerformanceAttempt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kết quả một lần chấm. {@code metrics} là JSON đầy đủ từ folkify_ai (chart, noteDeviations, đoạn đã chơi...). */
public record PerformanceResponse(
        UUID id,
        UUID songId,
        String songTitle,
        String instrumentSlug,
        int overall,
        int pitchScore,
        int rhythmScore,
        int stabilityScore,
        List<String> feedback,
        Map<String, Object> metrics,
        LocalDateTime createdAt
) {
    public static PerformanceResponse from(PerformanceAttempt a) {
        return new PerformanceResponse(
                a.getId(), a.getSong().getId(), a.getSong().getTitle(), a.getInstrument().getSlug(),
                a.getOverall(), a.getPitchScore(), a.getRhythmScore(), a.getStabilityScore(),
                a.getFeedback(), a.getMetrics(), a.getCreatedAt());
    }
}
