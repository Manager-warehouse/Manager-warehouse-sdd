package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.service.SystemConfigService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemConfigController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class SystemConfigControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SystemConfigService systemConfigService;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@wms.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setFullName("System Admin");
    }

    // ─── GET /api/v1/admin/system-config ─────────────────────────────────────

    @Test
    @DisplayName("GET /system-config — 200 OK khi ADMIN truy cập")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void getAllConfigs_admin_returns200() throws Exception {
        List<SystemConfigResponse> configs = List.of(
                SystemConfigResponse.builder().configKey("DEFAULT_CREDIT_LIMIT").configValue("10000000").build(),
                SystemConfigResponse.builder().configKey("MONTHLY_CLOSING_DAY").configValue("25").build()
        );
        when(systemConfigService.getAllConfigs()).thenReturn(configs);

        mockMvc.perform(get("/api/v1/admin/system-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].configKey").value("DEFAULT_CREDIT_LIMIT"))
                .andExpect(jsonPath("$[0].configValue").value("10000000"))
                .andExpect(jsonPath("$[1].configKey").value("MONTHLY_CLOSING_DAY"));
    }

    @Test
    @DisplayName("GET /system-config — 403 FORBIDDEN khi CEO truy cập")
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void getAllConfigs_ceo_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /system-config — 403 FORBIDDEN khi STOREKEEPER truy cập")
    @WithMockUser(username = "store@wms.com", roles = "STOREKEEPER")
    void getAllConfigs_storekeeper_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /system-config — 401 khi không có token")
    void getAllConfigs_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /system-config — Danh sách rỗng khi chưa có config nào")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void getAllConfigs_emptyList_returns200() throws Exception {
        when(systemConfigService.getAllConfigs()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/system-config").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── PUT /api/v1/admin/system-config/{configKey} ─────────────────────────

    @Test
    @DisplayName("PUT /system-config/{key} — 200 OK khi ADMIN cập nhật hợp lệ")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void updateConfig_admin_validValue_returns200() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(systemConfigService.updateConfig(eq("DEFAULT_CREDIT_LIMIT"), any(), eq(1L)))
                .thenReturn(SystemConfigResponse.builder()
                        .configKey("DEFAULT_CREDIT_LIMIT").configValue("500000000").build());

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("500000000");

        mockMvc.perform(put("/api/v1/admin/system-config/DEFAULT_CREDIT_LIMIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configKey").value("DEFAULT_CREDIT_LIMIT"))
                .andExpect(jsonPath("$.configValue").value("500000000"));
    }

    @Test
    @DisplayName("PUT /system-config/{key} — 403 FORBIDDEN khi CEO cập nhật")
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void updateConfig_ceo_returns403() throws Exception {
        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("500000000");

        mockMvc.perform(put("/api/v1/admin/system-config/DEFAULT_CREDIT_LIMIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /system-config/{key} — 401 khi không có token")
    void updateConfig_unauthenticated_returns403() throws Exception {
        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("500000000");

        mockMvc.perform(put("/api/v1/admin/system-config/DEFAULT_CREDIT_LIMIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /system-config/{key} — 400 khi service ném IllegalArgumentException (giá trị âm)")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void updateConfig_invalidValue_returns400() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(systemConfigService.updateConfig(eq("DEFAULT_CREDIT_LIMIT"), any(), eq(1L)))
                .thenThrow(new IllegalArgumentException("DEFAULT_CREDIT_LIMIT must be > 0"));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("-1000");

        mockMvc.perform(put("/api/v1/admin/system-config/DEFAULT_CREDIT_LIMIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /system-config/{key} — 404 khi configKey không tồn tại trong DB")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void updateConfig_keyNotFound_returns404() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(systemConfigService.updateConfig(eq("UNKNOWN_KEY"), any(), eq(1L)))
                .thenThrow(new ResourceNotFoundException("SystemConfig not found"));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("100");

        mockMvc.perform(put("/api/v1/admin/system-config/UNKNOWN_KEY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /system-config/{key} — 400 khi format số không hợp lệ")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void updateConfig_nonNumericValue_returns400() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(systemConfigService.updateConfig(eq("DEFAULT_CREDIT_LIMIT"), any(), eq(1L)))
                .thenThrow(new IllegalArgumentException("Invalid number format for key DEFAULT_CREDIT_LIMIT"));

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("abc");

        mockMvc.perform(put("/api/v1/admin/system-config/DEFAULT_CREDIT_LIMIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /system-config/MONTHLY_CLOSING_DAY — 200 OK với giá trị hợp lệ")
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void updateConfig_monthlyClosingDay_valid_returns200() throws Exception {
        when(userRepository.findByEmail("admin@wms.com")).thenReturn(Optional.of(adminUser));
        when(systemConfigService.updateConfig(eq("MONTHLY_CLOSING_DAY"), any(), eq(1L)))
                .thenReturn(SystemConfigResponse.builder()
                        .configKey("MONTHLY_CLOSING_DAY").configValue("25").build());

        SystemConfigUpdateRequest req = new SystemConfigUpdateRequest();
        req.setConfigValue("25");

        mockMvc.perform(put("/api/v1/admin/system-config/MONTHLY_CLOSING_DAY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configValue").value("25"));
    }
}
