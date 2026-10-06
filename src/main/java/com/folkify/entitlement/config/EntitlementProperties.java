package com.folkify.entitlement.config;

import com.folkify.auth.entity.Plan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Hạn mức theo gói, bind từ prefix "folkify.entitlement" trong application.yml. */
@Component
@ConfigurationProperties(prefix = "folkify.entitlement")
public class EntitlementProperties {

    /** Số lượt chấm điểm AI mỗi tháng theo gói. Thiếu key = 0 lượt. */
    private Map<Plan, Integer> aiMonthlyQuota = new EnumMap<>(Plan.class);

    /** Số ngày lưu lịch sử chấm điểm theo gói. Thiếu key = không giới hạn. */
    private Map<Plan, Integer> historyDays = new EnumMap<>(Plan.class);

    public Map<Plan, Integer> getAiMonthlyQuota() { return aiMonthlyQuota; }
    public void setAiMonthlyQuota(Map<Plan, Integer> aiMonthlyQuota) { this.aiMonthlyQuota = aiMonthlyQuota; }
    public Map<Plan, Integer> getHistoryDays() { return historyDays; }
    public void setHistoryDays(Map<Plan, Integer> historyDays) { this.historyDays = historyDays; }
}
