package com.wms.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.StockAlertResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.CurrentUserService;
import com.wms.service.StockAlertService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockAlertController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class StockAlertControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private StockAlertService stockAlertService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User planner;

    @BeforeEach
    void setUp() {
        planner = new User();
        planner.setId(8L);
        planner.setEmail("planner@wms.com");
        planner.setRole(UserRole.PLANNER);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void getLowStockAlerts_success() throws Exception {
        StockAlertResponse alert = StockAlertResponse.builder()
                .id(1L)
                .warehouseId(1L)
                .productId(100L)
                .currentQty(new BigDecimal("5"))
                .reorderPoint(new BigDecimal("20"))
                .isResolved(false)
                .build();

        PageImpl<StockAlertResponse> page = new PageImpl<>(List.of(alert));

        when(stockAlertService.getLowStockAlerts(eq(1L), eq(100L), anyBoolean(), anyInt(), anyInt(), eq(8L)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/alerts/low-stock")
                        .param("warehouseId", "1")
                        .param("productId", "100")
                        .param("isResolved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentQty").value(5))
                .andExpect(jsonPath("$.content[0].reorderPoint").value(20));
    }
}
