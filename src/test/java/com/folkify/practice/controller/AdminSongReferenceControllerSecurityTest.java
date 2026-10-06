package com.folkify.practice.controller;

import com.folkify.auth.repository.UserRepository;
import com.folkify.auth.service.JwtService;
import com.folkify.config.SecurityConfig;
import com.folkify.exception.GlobalExceptionHandler;
import com.folkify.practice.service.SongReferenceService;
import com.folkify.security.JwtAuthFilter;
import com.folkify.security.RestAccessDeniedHandler;
import com.folkify.security.RestAuthenticationEntryPoint;
import com.folkify.security.SecurityErrorResponder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Upload bản mẫu tác phẩm chỉ dành cho ADMIN. */
@WebMvcTest(AdminSongReferenceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class,
        SecurityErrorResponder.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminSongReferenceControllerSecurityTest {

    private static final String URL = "/api/admin/songs/0190f0a0-0000-7000-8000-000000000001/reference";
    private static final MockMultipartFile FILE = new MockMultipartFile("file", "ref.mp3", "audio/mpeg", new byte[]{1, 2});

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SongReferenceService songReferenceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void chuaDangNhap_tra401() throws Exception {
        mockMvc.perform(multipart(URL).file(FILE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userThuong_tra403() throws Exception {
        mockMvc.perform(multipart(URL).file(FILE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_tra200() throws Exception {
        mockMvc.perform(multipart(URL).file(FILE)).andExpect(status().isOk());
    }
}
