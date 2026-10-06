package com.folkify.practice.dto;

import com.folkify.practice.entity.PerformanceAttempt;

import java.time.LocalDateTime;
import java.util.UUID;

/** Dòng lịch sử chấm điểm (không kèm metrics nặng). */
public record PerformanceSummaryResponse(
        UUID id,
        String songTitle,
        String instrumentSlug,
        int overall,
        int pitchScore,
        int rhythmScore,
        int stabilityScore,
        Integer coveragePercent,
        LocalDateTime createdAt
) {
    public static PerformanceSummaryResponse from(PerformanceAttempt a) {
        Object coverage = a.getMetrics() != null ? a.getMetrics().get("coveragePercent") : null;
        return new PerformanceSummaryResponse(
                a.getId(), a.getSong().getTitle(), a.getInstrument().getSlug(),
                a.getOverall(), a.getPitchScore(), a.getRhythmScore(), a.getStabilityScore(),
                coverage instanceof Number n ? n.intValue() : null,
                a.getCreatedAt());
    }
}
