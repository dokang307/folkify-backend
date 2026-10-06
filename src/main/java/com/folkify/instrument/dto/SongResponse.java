package com.folkify.instrument.dto;

import com.folkify.auth.entity.Plan;
import com.folkify.instrument.entity.Song;

import java.util.UUID;

/**
 * Tác phẩm của nhạc cụ. {@code locked} = gói của user chưa đủ để AI chấm tác phẩm này
 * (khi đó không lộ audio mẫu). {@code scoringReady} = đã có bản mẫu để chấm.
 */
public record SongResponse(
        UUID id,
        String title,
        String artist,
        String duration,
        Plan requiredPlan,
        boolean locked,
        boolean scoringReady,
        String referenceAudioUrl,
        Double referenceDurationSeconds,
        String attribution,
        String sourceUrl
) {
    public static SongResponse from(Song song, boolean locked) {
        return new SongResponse(
                song.getId(), song.getTitle(), song.getArtist(), song.getDuration(),
                song.getRequiredPlan(), locked, song.isScoringReady(),
                locked ? null : song.getReferenceAudioUrl(),
                song.getReferenceDurationSeconds(), song.getAttribution(), song.getSourceUrl());
    }
}
