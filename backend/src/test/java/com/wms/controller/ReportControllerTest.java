package com.wms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.reporting_alerting.ReportController;
import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.reporting_alerting.ReportService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReportService reportService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User ceo;

    @BeforeEach
    void setUp() {
        ceo = new User();
        ceo.setId(1L);
        ceo.setEmail("ceo@wms.com");
        ceo.setRole(UserRole.CEO);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(ceo);
    }

    @Test
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void getCeoDashboard_success() throws Exception {
        CeoDashboardResponse.Kpis kpis = CeoDashboardResponse.Kpis.builder()
                .totalInventoryValue(new BigDecimal("12500000000"))
                .build();

        CeoDashboardResponse response = CeoDashboardResponse.builder()
                .kpis(kpis)
                .topDebtors(List.of())
                .build();

        when(reportService.getCeoDashboard(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/ceo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalInventoryValue").value(12500000000L));
    }

    @Test
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void getInventoryValuation_success() throws Exception {
        InventoryValuationResponse.Summary summary = InventoryValuationResponse.Summary.builder()
                .totalValuation(new BigDecimal("5000000000"))
                .build();

        InventoryValuationResponse res = InventoryValuationResponse.builder()
                .summary(summary)
                .records(List.of())
                .build();

        when(reportService.getInventoryValuation(eq(1L), eq(1L))).thenReturn(res);

        mockMvc.perform(get("/api/v1/reports/inventory-valuation").param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalValuation").value(5000000000L));
    }
}

