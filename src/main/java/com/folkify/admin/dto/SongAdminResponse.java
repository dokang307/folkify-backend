package com.folkify.admin.dto;

import com.folkify.auth.entity.Plan;
import com.folkify.instrument.entity.Song;

import java.util.UUID;

public record SongAdminResponse(
        UUID id,
        UUID instrumentId,
        String instrumentName,
        String title,
        String artist,
        String duration,
        int orderIndex,
        Plan requiredPlan,
        boolean scoringReady,
        String referenceAudioUrl,
        Double referenceDurationSeconds,
        String sourceUrl,
        String attribution
) {
    public static SongAdminResponse from(Song s) {
        return new SongAdminResponse(
                s.getId(),
                s.getInstrument().getId(),
                s.getInstrument().getName(),
                s.getTitle(), s.getArtist(), s.getDuration(), s.getOrderIndex(),
                s.getRequiredPlan(), s.isScoringReady(), s.getReferenceAudioUrl(),
                s.getReferenceDurationSeconds(), s.getSourceUrl(), s.getAttribution()
        );
    }
}
