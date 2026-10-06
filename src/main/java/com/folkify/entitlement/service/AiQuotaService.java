package com.folkify.entitlement.service;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.entitlement.dto.AiQuotaDto;
import com.folkify.entitlement.dto.EntitlementsResponse;
import com.folkify.entitlement.entity.UserAiUsage;
import com.folkify.entitlement.entity.UserAiUsageId;
import com.folkify.entitlement.repository.UserAiUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Đếm và kiểm tra lượt chấm điểm AI theo tháng lịch UTC. */
@Service
public class AiQuotaService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserAiUsageRepository usageRepository;
    private final PlanPolicy planPolicy;
    private final Clock clock;

    public AiQuotaService(UserAiUsageRepository usageRepository, PlanPolicy planPolicy, Clock clock) {
        this.usageRepository = usageRepository;
        this.planPolicy = planPolicy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiQuotaDto status(User user) {
        int limit = planPolicy.aiMonthlyQuota(planPolicy.effectivePlan(user));
        int used = usageRepository.findById(currentId(user))
                .map(UserAiUsage::getUsedCount)
                .orElse(0);
        return new AiQuotaDto(limit, used, Math.max(0, limit - used), resetsAt());
    }

    @Transactional(readOnly = true)
    public EntitlementsResponse entitlements(User user) {
        Plan plan = planPolicy.effectivePlan(user);
        var history = planPolicy.historyDays(plan);
        return new EntitlementsResponse(
                plan,
                plan == Plan.FREE ? null : user.getPlanExpiresAt(),
                status(user),
                history.isPresent() ? history.getAsInt() : null);
    }

    /** Kiểm tra sớm trước khi tốn công phân tích audio. Ném 1310 / 1311. */
    @Transactional(readOnly = true)
    public void assertAvailable(User user) {
        AiQuotaDto quota = status(user);
        if (quota.limit() <= 0) {
            throw new ApiException(ErrorCode.AI_NOT_IN_PLAN);
        }
        if (quota.remaining() <= 0) {
            throw new ApiException(ErrorCode.AI_QUOTA_EXCEEDED);
        }
    }

    /**
     * Trừ 1 lượt. Phải được gọi trong cùng transaction lưu kết quả chấm điểm thành công,
     * để lỗi phân tích không bị tính lượt. Khóa row để tránh vượt quota khi gửi đồng thời.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(User user) {
        int limit = planPolicy.aiMonthlyQuota(planPolicy.effectivePlan(user));
        if (limit <= 0) {
            throw new ApiException(ErrorCode.AI_NOT_IN_PLAN);
        }
        UserAiUsageId id = currentId(user);
        usageRepository.insertIfAbsent(id.getUserId(), id.getPeriod());
        UserAiUsage usage = usageRepository.findForUpdate(id)
                .orElseThrow(() -> new ApiException(ErrorCode.UNEXPECTED_ERROR));
        if (usage.getUsedCount() >= limit) {
            throw new ApiException(ErrorCode.AI_QUOTA_EXCEEDED);
        }
        usage.increment();
    }

    private UserAiUsageId currentId(User user) {
        return new UserAiUsageId(user.getId(), YearMonth.now(clock.withZone(ZoneOffset.UTC)).format(PERIOD_FORMAT));
    }

    private Instant resetsAt() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).plusMonths(1)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
