package com.folkify.entitlement.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.dto.AiQuotaDto;
import com.folkify.entitlement.entity.UserAiUsage;
import com.folkify.entitlement.entity.UserAiUsageId;
import com.folkify.entitlement.repository.UserAiUsageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiQuotaServiceTest {

    UserAiUsageRepository repo = Mockito.mock(UserAiUsageRepository.class);
    AiQuotaService service = new AiQuotaService(repo, PlanPolicyTest.policy(), PlanPolicyTest.CLOCK);

    static User user(Plan plan) {
        User u = PlanPolicyTest.user(plan, PlanPolicyTest.NOW.plusDays(10));
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    void used(User u, int count) {
        UserAiUsage usage = new UserAiUsage(new UserAiUsageId(u.getId(), "2026-10"), count);
        when(repo.findById(new UserAiUsageId(u.getId(), "2026-10"))).thenReturn(Optional.of(usage));
        when(repo.findForUpdate(new UserAiUsageId(u.getId(), "2026-10"))).thenReturn(Optional.of(usage));
    }

    void assertCode(Runnable r, ErrorCode code) {
        assertThatThrownBy(r::run).isInstanceOf(ApiException.class).extracting("errorCode").isEqualTo(code);
    }

    @Test
    void free_isNotInPlan() {
        User u = user(Plan.FREE);
        assertCode(() -> service.assertAvailable(u), ErrorCode.AI_NOT_IN_PLAN);
        assertCode(() -> service.consume(u), ErrorCode.AI_NOT_IN_PLAN);
    }

    @Test
    void basicWithRemaining_consumesOne() {
        User u = user(Plan.BASIC);
        used(u, 9);
        service.assertAvailable(u);
        service.consume(u);
        verify(repo).insertIfAbsent(u.getId(), "2026-10");
        assertThat(service.status(u).used()).isEqualTo(10);
    }

    @Test
    void basicAtLimit_isExceeded() {
        User u = user(Plan.BASIC);
        used(u, 10);
        assertCode(() -> service.assertAvailable(u), ErrorCode.AI_QUOTA_EXCEEDED);
        assertCode(() -> service.consume(u), ErrorCode.AI_QUOTA_EXCEEDED);
    }

    @Test
    void proStatus_reportsLimitRemainingAndReset() {
        User u = user(Plan.PRO);
        used(u, 3);
        AiQuotaDto q = service.status(u);
        assertThat(q.limit()).isEqualTo(50);
        assertThat(q.remaining()).isEqualTo(47);
        assertThat(q.resetsAt()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void noUsageRow_meansZeroUsed() {
        User u = user(Plan.PRO);
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThat(service.status(u).used()).isZero();
    }

    @Test
    void entitlements_includeHistoryWindowForBasicOnly() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThat(service.entitlements(user(Plan.BASIC)).historyDays()).isEqualTo(30);
        assertThat(service.entitlements(user(Plan.PRO)).historyDays()).isNull();
    }
}
