package com.folkify.entitlement.controller;

import com.folkify.auth.repository.UserRepository;
import com.folkify.auth.service.JwtService;
import com.folkify.config.SecurityConfig;
import com.folkify.entitlement.service.AiQuotaService;
import com.folkify.entitlement.service.PlanPolicy;
import com.folkify.payment.config.PayOsProperties;
import com.folkify.exception.GlobalExceptionHandler;
import com.folkify.security.JwtAuthFilter;
import com.folkify.security.RestAccessDeniedHandler;
import com.folkify.security.RestAuthenticationEntryPoint;
import com.folkify.security.SecurityErrorResponder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/me/entitlements bắt buộc đăng nhập. */
@WebMvcTest(EntitlementController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class,
        SecurityErrorResponder.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class EntitlementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiQuotaService aiQuotaService;

    @MockitoBean
    private PlanPolicy planPolicy;

    @MockitoBean
    private PayOsProperties payOsProperties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void chuaDangNhap_tra401() throws Exception {
        mockMvc.perform(get("/api/me/entitlements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void bangGia_congKhai() throws Exception {
        mockMvc.perform(get("/api/plans")).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(3));
    }
}
