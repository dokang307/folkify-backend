package com.folkify.payment.config;

import com.folkify.auth.entity.Plan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** Cấu hình cổng thanh toán PayOS, bind từ prefix "payos" trong application.yml. */
@Component
@ConfigurationProperties(prefix = "payos")
public class PayOsProperties {

    /** Client ID lấy từ dashboard PayOS. */
    private String clientId;
    /** API Key lấy từ dashboard PayOS. */
    private String apiKey;
    /** Checksum Key dùng để ký & xác thực chữ ký webhook. */
    private String checksumKey;

    /** URL PayOS redirect trình duyệt về khi thanh toán thành công. */
    private String returnUrl;
    /** URL PayOS redirect trình duyệt về khi user hủy thanh toán. */
    private String cancelUrl;

    /** URL PayOS redirect về web app khi thanh toán thành công / hủy (luồng thanh toán từ trình duyệt). */
    private String webReturnUrl;
    private String webCancelUrl;

    /** Bảng giá theo gói (VND). Ví dụ: BASIC=49000, PRO=99000. */
    private Map<Plan, Long> planPrices = new EnumMap<>(Plan.class);

    /** Số ngày hiệu lực mỗi lần mua/gia hạn gói. */
    private int planDurationDays = 30;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getChecksumKey() { return checksumKey; }
    public void setChecksumKey(String checksumKey) { this.checksumKey = checksumKey; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public String getWebReturnUrl() { return webReturnUrl; }
    public void setWebReturnUrl(String webReturnUrl) { this.webReturnUrl = webReturnUrl; }
    public String getWebCancelUrl() { return webCancelUrl; }
    public void setWebCancelUrl(String webCancelUrl) { this.webCancelUrl = webCancelUrl; }
    public Map<Plan, Long> getPlanPrices() { return planPrices; }
    public void setPlanPrices(Map<Plan, Long> planPrices) { this.planPrices = planPrices; }
    public int getPlanDurationDays() { return planDurationDays; }
    public void setPlanDurationDays(int planDurationDays) { this.planDurationDays = planDurationDays; }
}
