package com.folkify.payment.dto.request;

import com.folkify.payment.enumType.CheckoutPlatform;
import com.folkify.auth.entity.Plan;
import jakarta.validation.constraints.NotNull;

/** Body app gửi lên khi bấm "Nâng cấp". */
public record CheckoutRequest(
        @NotNull(message = "Vui lòng chọn gói cần nâng cấp") Plan plan,
        /** Mặc định APP để client mobile cũ không phải đổi gì. */
        CheckoutPlatform platform) {

    public CheckoutPlatform platformOrDefault() {
        return platform != null ? platform : CheckoutPlatform.APP;
    }
}
