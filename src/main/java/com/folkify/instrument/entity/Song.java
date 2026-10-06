package com.folkify.instrument.entity;

import com.folkify.auth.entity.Plan;
import com.folkify.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/** Tác phẩm (bài nhạc) của một nhạc cụ — cũng là đơn vị để AI chấm phần trình diễn. */
@Entity
@Table(name = "songs")
public class Song extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_song_instrument"))
    private Instrument instrument;

    @Column(nullable = false)
    private String title;

    private String artist;
    private String duration;
    private int orderIndex;

    /** Gói tối thiểu để được AI chấm tác phẩm này. */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_plan", nullable = false, length = 10)
    private Plan requiredPlan = Plan.BASIC;

    @Column(name = "reference_audio_url", columnDefinition = "TEXT")
    private String referenceAudioUrl;

    /** Đường cao độ do folkify_ai trích từ bản mẫu ({hopSeconds, times[], midi[], durationSeconds}). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reference_contour", columnDefinition = "jsonb")
    private Map<String, Object> referenceContour;

    @Column(name = "reference_duration_seconds")
    private Double referenceDurationSeconds;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String attribution;

    public Song() {}

    public Instrument getInstrument() { return instrument; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public int getOrderIndex() { return orderIndex; }
    public Plan getRequiredPlan() { return requiredPlan; }
    public String getReferenceAudioUrl() { return referenceAudioUrl; }
    public Map<String, Object> getReferenceContour() { return referenceContour; }
    public Double getReferenceDurationSeconds() { return referenceDurationSeconds; }
    public String getSourceUrl() { return sourceUrl; }
    public String getAttribution() { return attribution; }

    /** Đã có bản mẫu đã phân tích → chấm điểm được. */
    public boolean isScoringReady() { return referenceContour != null; }

    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public void setRequiredPlan(Plan requiredPlan) { this.requiredPlan = requiredPlan != null ? requiredPlan : Plan.BASIC; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setAttribution(String attribution) { this.attribution = attribution; }

    public void attachReference(String audioUrl, Map<String, Object> contour, Double durationSeconds) {
        this.referenceAudioUrl = audioUrl;
        this.referenceContour = contour;
        this.referenceDurationSeconds = durationSeconds;
    }
}
