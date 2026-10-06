package com.folkify.instrument.dto;

import com.folkify.auth.entity.Plan;
import com.folkify.instrument.entity.Lesson;

import java.util.List;
import java.util.UUID;

public record LessonDetailResponse(
        UUID id,
        String slug,
        String title,
        String duration,
        String level,
        String description,
        List<String> steps,
        List<String> tips,
        int xp,
        String youtubeUrl,
        String youtubeVideoId,
        String channelName,
        String sourceUrl,
        Plan requiredPlan
) {
    public static LessonDetailResponse from(Lesson lesson) {
        return new LessonDetailResponse(
                lesson.getId(), lesson.getSlug(), lesson.getTitle(),
                lesson.getDuration(), lesson.getLevel(), lesson.getDescription(),
                lesson.getSteps(), lesson.getTips(), lesson.getXp(), lesson.getYoutubeUrl(),
                lesson.getYoutubeVideoId(), lesson.getChannelName(), lesson.getSourceUrl(),
                lesson.getRequiredPlan()
        );
    }
}
