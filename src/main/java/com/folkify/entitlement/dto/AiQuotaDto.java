package com.folkify.entitlement.dto;

import java.time.Instant;

/** Lượt chấm điểm AI trong kỳ hiện tại. {@code resetsAt} = đầu tháng sau (UTC). */
public record AiQuotaDto(int limit, int used, int remaining, Instant resetsAt) {}
