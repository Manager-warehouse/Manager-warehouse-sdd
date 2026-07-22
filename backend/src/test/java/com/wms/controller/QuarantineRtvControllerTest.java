package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.RtvActionResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.CurrentUserService;
import com.wms.service.QuarantineRtvService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuarantineRtvController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class QuarantineRtvControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private QuarantineRtvService quarantineRtvService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User whManager;

    @BeforeEach
    void setUp() {
        whManager = new User();
        whManager.setId(15L);
        whManager.setEmail("wh_mgr@wms.com");
        whManager.setRole(UserRole.WAREHOUSE_MANAGER);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(whManager);
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void createRtv_success() throws Exception {
        RtvActionResponse response = RtvActionResponse.builder()
                .adjustmentId(200L)
                .debitNoteNumber("DN-2026-001")
                .confirmed(false)
                .build();

        when(quarantineRtvService.createRtv(eq(200L), any(), eq(whManager))).thenReturn(response);

        String json = "{\"reason\":\"Defective batch\",\"expectedVersion\":0}";

        mockMvc.perform(post("/api/v1/receipts/200/rtv")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.debitNoteNumber").value("DN-2026-001"));
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "STOREKEEPER")
    void confirmRtv_success() throws Exception {
        RtvActionResponse response = RtvActionResponse.builder()
                .adjustmentId(200L)
                .confirmed(true)
                .build();

        when(quarantineRtvService.confirmRtv(eq(200L), any(), any())).thenReturn(response);

        String json = "{\"returnedQty\":50,\"expectedVersion\":1}";

        mockMvc.perform(put("/api/v1/receipts/200/rtv/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));
    }
}
