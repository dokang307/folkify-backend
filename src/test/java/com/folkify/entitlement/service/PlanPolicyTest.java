package com.folkify.entitlement.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.entitlement.config.EntitlementProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PlanPolicyTest {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
    public static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    public static PlanPolicy policy() {
        EntitlementProperties props = new EntitlementProperties();
        props.setAiMonthlyQuota(new EnumMap<>(Map.of(Plan.FREE, 0, Plan.BASIC, 10, Plan.PRO, 50)));
        props.setHistoryDays(new EnumMap<>(Map.of(Plan.BASIC, 30)));
        return new PlanPolicy(props, CLOCK);
    }

    public static User user(Plan plan, LocalDateTime expiresAt) {
        User u = new User.Builder().name("u").email("u@x.vn").password("p").plan(plan).build();
        u.setPlanExpiresAt(expiresAt);
        return u;
    }

    @Test
    void anonymous_isFree() {
        assertThat(policy().effectivePlan(null)).isEqualTo(Plan.FREE);
    }

    @Test
    void activePaidPlan_isKept() {
        assertThat(policy().effectivePlan(user(Plan.BASIC, NOW.plusDays(3)))).isEqualTo(Plan.BASIC);
    }

    @Test
    void expiredPaidPlan_fallsBackToFree() {
        assertThat(policy().effectivePlan(user(Plan.PRO, NOW.minusDays(1)))).isEqualTo(Plan.FREE);
    }

    @Test
    void canAccess_respectsPlanOrder() {
        PlanPolicy p = policy();
        assertThat(p.canAccess(user(Plan.BASIC, NOW.plusDays(1)), Plan.PRO)).isFalse();
        assertThat(p.canAccess(user(Plan.PRO, NOW.plusDays(1)), Plan.BASIC)).isTrue();
        assertThat(p.canAccess(null, Plan.FREE)).isTrue();
        assertThat(p.canAccess(null, Plan.BASIC)).isFalse();
    }

    @Test
    void quotaAndHistory_comeFromProperties() {
        PlanPolicy p = policy();
        assertThat(p.aiMonthlyQuota(Plan.FREE)).isZero();
        assertThat(p.aiMonthlyQuota(Plan.BASIC)).isEqualTo(10);
        assertThat(p.aiMonthlyQuota(Plan.PRO)).isEqualTo(50);
        assertThat(p.historyDays(Plan.BASIC)).hasValue(30);
        assertThat(p.historyDays(Plan.PRO)).isEmpty();
    }
}
