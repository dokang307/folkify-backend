package com.folkify.entitlement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserAiUsageId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Kỳ tính quota dạng yyyy-MM (UTC). */
    @Column(name = "period", nullable = false, length = 7)
    private String period;

    protected UserAiUsageId() {}

    public UserAiUsageId(UUID userId, String period) {
        this.userId = userId;
        this.period = period;
    }

    public UUID getUserId() { return userId; }
    public String getPeriod() { return period; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAiUsageId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(period, that.period);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, period); }
}
