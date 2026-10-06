package com.folkify.entitlement.dto;

import com.folkify.auth.entity.Plan;

/** Thông tin gói công khai cho trang bảng giá. {@code historyDays} null = không giới hạn. */
public record PlanInfoResponse(Plan plan, long priceVnd, int durationDays, int aiMonthlyQuota, Integer historyDays) {}
