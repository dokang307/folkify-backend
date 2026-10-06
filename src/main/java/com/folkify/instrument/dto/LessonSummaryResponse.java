package com.folkify.instrument.dto;

import com.folkify.auth.entity.Plan;
import com.folkify.instrument.entity.Lesson;

import java.util.UUID;

/** Tóm tắt bài học; không chứa video để bài bị khóa không lộ nội dung. */
public record LessonSummaryResponse(
        UUID id,
        String slug,
        String title,
        String duration,
        String level,
        int xp,
        int orderIndex,
        Plan requiredPlan,
        boolean locked,
        boolean completed
) {
    public static LessonSummaryResponse from(Lesson lesson, boolean locked) {
        return from(lesson, locked, false);
    }

    public static LessonSummaryResponse from(Lesson lesson, boolean locked, boolean completed) {
        return new LessonSummaryResponse(
                lesson.getId(), lesson.getSlug(), lesson.getTitle(),
                lesson.getDuration(), lesson.getLevel(), lesson.getXp(), lesson.getOrderIndex(),
                lesson.getRequiredPlan(), locked, completed
        );
    }
}
