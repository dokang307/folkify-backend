package com.folkify.entitlement.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Bộ đếm lượt chấm điểm AI của một user trong một tháng. */
@Entity
@Table(name = "user_ai_usage")
public class UserAiUsage {

    @EmbeddedId
    private UserAiUsageId id;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserAiUsage() {}

    public UserAiUsage(UserAiUsageId id, int usedCount) {
        this.id = id;
        this.usedCount = usedCount;
    }

    public UserAiUsageId getId() { return id; }
    public int getUsedCount() { return usedCount; }
    public void increment() { this.usedCount++; }
}
