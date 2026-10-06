package com.folkify.practice.client;

import java.util.List;
import java.util.Map;

/** Kết quả chấm điểm từ folkify_ai. {@code raw} giữ nguyên toàn bộ JSON để lưu làm metrics. */
public record AiAnalysis(int overall, int pitchScore, int rhythmScore, int stabilityScore,
                         List<String> feedback, Map<String, Object> raw) {

    @SuppressWarnings("unchecked")
    public static AiAnalysis from(Map<String, Object> json) {
        Object feedback = json.get("feedback");
        return new AiAnalysis(
                ((Number) json.get("overall")).intValue(),
                ((Number) json.get("pitchScore")).intValue(),
                ((Number) json.get("rhythmScore")).intValue(),
                ((Number) json.get("stabilityScore")).intValue(),
                feedback instanceof List<?> list ? (List<String>) list : List.of(),
                json);
    }
}
