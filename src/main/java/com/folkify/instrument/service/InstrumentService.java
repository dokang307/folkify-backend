package com.folkify.instrument.service;

import com.folkify.auth.entity.User;
import com.folkify.instrument.dto.*;

import java.util.List;

public interface InstrumentService {
    List<InstrumentSummaryResponse> getAllInstruments();
    /** {@code user} có thể null (khách vãng lai) — dùng để tính trạng thái khóa bài học. */
    InstrumentDetailResponse getInstrumentBySlug(String slug, User user);
    List<LessonSummaryResponse> getLessonsByInstrument(String slug, User user);
    /** Ném PLAN_REQUIRED nếu gói của user chưa đủ để xem bài. */
    LessonDetailResponse getLessonDetail(String instrumentSlug, String lessonSlug, User user);
    /** {@code user} có thể null — dùng để tính trạng thái khóa AI chấm điểm của từng tác phẩm. */
    List<SongResponse> getSongsByInstrument(String slug, User user);
}
