package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.return_disposal.DisposalController;
import com.wms.dto.response.DisposalResponse;
import com.wms.dto.response.PendingDisposalResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.return_disposal.DisposalService;
import com.wms.service.user_context.CurrentUserService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DisposalController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class DisposalControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DisposalService disposalService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User warehouseManager;

    @BeforeEach
    void setUp() {
        warehouseManager = new User();
        warehouseManager.setId(12L);
        warehouseManager.setEmail("wh_mgr@wms.com");
        warehouseManager.setRole(UserRole.WAREHOUSE_MANAGER);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseManager);
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void createDisposal_success() throws Exception {
        DisposalResponse res = DisposalResponse.builder()
                .adjustmentId(501L)
                .adjustmentNumber("ADJ-DIS-001")
                .autoApproved(true)
                .message("Disposal request auto-approved")
                .build();

        when(disposalService.createDisposalRequest(eq(10L), any(), eq(warehouseManager))).thenReturn(res);

        String body = "{\"cause\":\"QC Damaged beyond repair\"}";

        mockMvc.perform(post("/api/v1/receipts/10/dispose")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustmentId").value(501))
                .andExpect(jsonPath("$.autoApproved").value(true));
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void getPendingDisposals_success() throws Exception {
        PendingDisposalResponse item = PendingDisposalResponse.builder()
                .id(88L)
                .productName("Chao Chong Dinh")
                .failedQty(new BigDecimal("100"))
                .totalValue(new BigDecimal("120000000"))
                .build();

        when(disposalService.getPendingDisposals(eq(warehouseManager))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/disposals/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(88));
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void approveDisposal_success() throws Exception {
        DisposalResponse res = DisposalResponse.builder()
                .adjustmentId(88L)
                .autoApproved(false)
                .message("Approved by Manager")
                .build();

        when(disposalService.approveDisposal(eq(88L), eq(warehouseManager))).thenReturn(res);

        mockMvc.perform(put("/api/v1/disposal/88/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjustmentId").value(88));
    }
}
