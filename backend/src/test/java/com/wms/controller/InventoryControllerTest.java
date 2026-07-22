package com.wms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.InventoryService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private InventoryService inventoryService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void getAvailability_success() throws Exception {
        InventoryAvailabilityResponse res = new InventoryAvailabilityResponse(
                1L, 100L, new BigDecimal("500"), new BigDecimal("50"), new BigDecimal("450"));

        when(inventoryService.getAvailability(eq(1L), eq(100L))).thenReturn(res);

        mockMvc.perform(get("/api/v1/warehouse-stock/availability")
                        .param("warehouseId", "1")
                        .param("productId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQty").value(450));
    }

    @Test
    @WithMockUser(username = "wh_mgr@wms.com", roles = "WAREHOUSE_MANAGER")
    void getOverview_success() throws Exception {
        WarehouseStockOverviewResponse res = new WarehouseStockOverviewResponse(
                1L, new BigDecimal("5000"), 10L, 15L, 2L);

        when(inventoryService.getOverview(eq(1L))).thenReturn(res);

        mockMvc.perform(get("/api/v1/warehouse-stock/overview")
                        .param("warehouseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayReceiptCount").value(10));
    }
}
