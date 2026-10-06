package com.folkify.entitlement.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.entitlement.config.EntitlementProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.OptionalInt;

/** Nguồn sự thật duy nhất về thứ tự gói, gói hiệu lực và hạn mức theo gói. */
@Component
public class PlanPolicy {

    private final EntitlementProperties properties;
    private final Clock clock;

    public PlanPolicy(EntitlementProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Gói đang có hiệu lực: khách vãng lai hoặc gói trả phí đã hết hạn được coi là FREE. */
    public Plan effectivePlan(User user) {
        if (user == null || user.getPlan() == null) {
            return Plan.FREE;
        }
        LocalDateTime expiresAt = user.getPlanExpiresAt();
        if (user.getPlan() != Plan.FREE && expiresAt != null && expiresAt.isBefore(LocalDateTime.now(clock))) {
            return Plan.FREE;
        }
        return user.getPlan();
    }

    public boolean canAccess(User user, Plan required) {
        Plan needed = required != null ? required : Plan.FREE;
        return effectivePlan(user).ordinal() >= needed.ordinal();
    }

    public int aiMonthlyQuota(Plan plan) {
        return properties.getAiMonthlyQuota().getOrDefault(plan, 0);
    }

    public OptionalInt historyDays(Plan plan) {
        Integer days = properties.getHistoryDays().get(plan);
        return days == null ? OptionalInt.empty() : OptionalInt.of(days);
    }
}
