package com.folkify.instrument.entity;

import com.folkify.auth.entity.Plan;
import com.folkify.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_lesson_instrument"))
    private Instrument instrument;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String title;

    private String duration;
    private String level;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "lesson_steps", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "step", columnDefinition = "TEXT")
    @OrderColumn(name = "order_index")
    private List<String> steps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "lesson_tips", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "tip", columnDefinition = "TEXT")
    @OrderColumn(name = "order_index")
    private List<String> tips = new ArrayList<>();

    private int xp;
    private String youtubeUrl;
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_plan", nullable = false, length = 10)
    private Plan requiredPlan = Plan.FREE;

    @Column(name = "youtube_video_id", length = 20)
    private String youtubeVideoId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    public Lesson() {}

    public Instrument getInstrument() { return instrument; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDuration() { return duration; }
    public String getLevel() { return level; }
    public String getDescription() { return description; }
    public List<String> getSteps() { return steps; }
    public List<String> getTips() { return tips; }
    public int getXp() { return xp; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public int getOrderIndex() { return orderIndex; }
    public Plan getRequiredPlan() { return requiredPlan; }
    public String getYoutubeVideoId() { return youtubeVideoId; }
    public String getChannelName() { return channelName; }
    public String getSourceUrl() { return sourceUrl; }

    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    public void setSlug(String slug) { this.slug = slug; }
    public void setTitle(String title) { this.title = title; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setLevel(String level) { this.level = level; }
    public void setDescription(String description) { this.description = description; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void setTips(List<String> tips) { this.tips = tips; }
    public void setXp(int xp) { this.xp = xp; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public void setRequiredPlan(Plan requiredPlan) { this.requiredPlan = requiredPlan != null ? requiredPlan : Plan.FREE; }
    public void setYoutubeVideoId(String youtubeVideoId) { this.youtubeVideoId = youtubeVideoId; }
    public void setChannelName(String channelName) { this.channelName = channelName; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
}
