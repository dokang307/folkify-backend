package com.folkify.admin.dto;

import com.folkify.auth.entity.Plan;
import com.folkify.instrument.entity.Lesson;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LessonAdminResponse(
        UUID id,
        UUID instrumentId,
        String instrumentName,
        String slug,
        String title,
        String duration,
        String level,
        String description,
        List<String> steps,
        List<String> tips,
        int xp,
        String youtubeUrl,
        int orderIndex,
        Plan requiredPlan,
        String youtubeVideoId,
        String channelName,
        String sourceUrl
) {
    public static LessonAdminResponse from(Lesson l) {
        return new LessonAdminResponse(
                l.getId(),
                l.getInstrument().getId(),
                l.getInstrument().getName(),
                l.getSlug(), l.getTitle(), l.getDuration(), l.getLevel(),
                l.getDescription(), new ArrayList<>(l.getSteps()), new ArrayList<>(l.getTips()),
                l.getXp(), l.getYoutubeUrl(), l.getOrderIndex(),
                l.getRequiredPlan(), l.getYoutubeVideoId(), l.getChannelName(), l.getSourceUrl()
        );
    }
}
