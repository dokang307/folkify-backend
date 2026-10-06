package com.folkify.entitlement.dto;

import com.folkify.auth.entity.Plan;

import java.time.LocalDateTime;

/** Quyền lợi hiện tại của user: gói hiệu lực, hạn gói, quota AI, số ngày lưu lịch sử (null = không giới hạn). */
public record EntitlementsResponse(
        Plan plan,
        LocalDateTime planExpiresAt,
        AiQuotaDto aiQuota,
        Integer historyDays
) {}
