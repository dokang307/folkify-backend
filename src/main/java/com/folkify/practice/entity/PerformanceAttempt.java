package com.folkify.practice.entity;

import com.folkify.auth.entity.User;
import com.folkify.infrastructure.persistence.BaseEntity;
import com.folkify.instrument.entity.Instrument;
import com.folkify.instrument.entity.Song;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/** Một lần AI chấm phần trình diễn một tác phẩm. File ghi âm của học viên KHÔNG được lưu (chỉ lưu kết quả). */
@Entity
@Table(name = "performance_attempts")
public class PerformanceAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false)
    private int overall;

    @Column(name = "pitch_score", nullable = false)
    private int pitchScore;

    @Column(name = "rhythm_score", nullable = false)
    private int rhythmScore;

    @Column(name = "stability_score", nullable = false)
    private int stabilityScore;

    /** Kết quả đầy đủ từ folkify_ai (chart, noteDeviations, matchedStart/End, coveragePercent...). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> feedback;

    protected PerformanceAttempt() {}

    public PerformanceAttempt(User user, Song song, int overall, int pitchScore, int rhythmScore, int stabilityScore,
                              Map<String, Object> metrics, List<String> feedback) {
        this.user = user;
        this.song = song;
        this.instrument = song.getInstrument();
        this.overall = overall;
        this.pitchScore = pitchScore;
        this.rhythmScore = rhythmScore;
        this.stabilityScore = stabilityScore;
        this.metrics = metrics;
        this.feedback = feedback;
    }

    public User getUser() { return user; }
    public Song getSong() { return song; }
    public Instrument getInstrument() { return instrument; }
    public int getOverall() { return overall; }
    public int getPitchScore() { return pitchScore; }
    public int getRhythmScore() { return rhythmScore; }
    public int getStabilityScore() { return stabilityScore; }
    public Map<String, Object> getMetrics() { return metrics; }
    public List<String> getFeedback() { return feedback; }
}
