package com.folkify.practice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cấu hình chấm điểm, bind từ prefix "folkify.ai". */
@Component
@ConfigurationProperties(prefix = "folkify.ai")
public class PracticeProperties {

    /** URL nội bộ của service folkify_ai. */
    private String baseUrl;
    /** Timeout gọi folkify_ai (ms) — phân tích bản ghi 90s mất vài giây. */
    private int timeoutMs = 180000;
    /** Dung lượng tối đa file ghi âm/bản mẫu (byte). */
    private long maxUploadBytes = 25L * 1024 * 1024;
    /** Số bản ghi tối đa mỗi trang lịch sử. */
    private int maxPageSize = 50;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public long getMaxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
    public int getMaxPageSize() { return maxPageSize; }
    public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
}
