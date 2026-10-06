package com.folkify.entitlement.controller;

import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.response.ApiResponse;
import com.folkify.entitlement.dto.EntitlementsResponse;
import com.folkify.entitlement.dto.PlanInfoResponse;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.payment.config.PayOsProperties;
import com.folkify.entitlement.service.AiQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Entitlements", description = "Quyền lợi theo gói của người dùng")
public class EntitlementController {

    private final AiQuotaService aiQuotaService;
    private final PlanPolicy planPolicy;
    private final PayOsProperties payOsProperties;

    public EntitlementController(AiQuotaService aiQuotaService, PlanPolicy planPolicy, PayOsProperties payOsProperties) {
        this.aiQuotaService = aiQuotaService;
        this.planPolicy = planPolicy;
        this.payOsProperties = payOsProperties;
    }

    @GetMapping("/plans")
    @Operation(summary = "Bảng giá + quyền lợi từng gói (công khai)")
    public ResponseEntity<ApiResponse<List<PlanInfoResponse>>> getPlans() {
        List<PlanInfoResponse> plans = Arrays.stream(Plan.values())
                .map(p -> {
                    var history = planPolicy.historyDays(p);
                    return new PlanInfoResponse(p,
                            payOsProperties.getPlanPrices().getOrDefault(p, 0L),
                            payOsProperties.getPlanDurationDays(),
                            planPolicy.aiMonthlyQuota(p),
                            history.isPresent() ? history.getAsInt() : null);
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/me/entitlements")
    @Operation(summary = "Gói hiệu lực, hạn gói và lượt chấm điểm AI còn lại")
    public ResponseEntity<ApiResponse<EntitlementsResponse>> getEntitlements(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(aiQuotaService.entitlements(user)));
    }
}
