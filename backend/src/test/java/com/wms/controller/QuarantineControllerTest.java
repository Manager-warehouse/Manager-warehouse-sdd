package com.wms.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.dto.response.QuarantineItemResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.service.CurrentUserService;
import com.wms.service.QuarantineRtvService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.wms.config.SecurityConfig;
import com.wms.config.JwtAuthFilter;
import com.wms.exception.GlobalExceptionHandler;

@WebMvcTest(QuarantineController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class QuarantineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private QuarantineRtvService quarantineRtvService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private com.wms.repository.UserRepository userRepository;

    @Mock
    private com.wms.util.JwtUtil jwtUtil;

    @Mock
    private com.wms.config.UserDetailsServiceImpl userDetailsService;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(10L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void getQuarantineItems_success() throws Exception {
        QuarantineItemResponse item = QuarantineItemResponse.builder()
                .id(200L)
                .productSku("SKU-PA-001")
                .productName("Device A")
                .qcFailedQty(5)
                .qcFailureReason("Cracked")
                .receiptNumber("RCV-001")
                .supplierId(30L)
                .totalValue(BigDecimal.valueOf(50.0))
                .unit("cái")
                .build();

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(quarantineRtvService.getQuarantineItems(eq(1L), eq(manager)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/quarantine/items")
                .param("warehouseId", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].product_sku").value("SKU-PA-001"))
                .andExpect(jsonPath("$[0].product_name").value("Device A"))
                .andExpect(jsonPath("$[0].qc_failed_qty").value(5))
                .andExpect(jsonPath("$[0].qc_failure_reason").value("Cracked"))
                .andExpect(jsonPath("$[0].receipt_number").value("RCV-001"))
                .andExpect(jsonPath("$[0].supplier_id").value(30))
                .andExpect(jsonPath("$[0].total_value").value(50.0))
                .andExpect(jsonPath("$[0].unit").value("cái"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void getQuarantineItems_missingWarehouseId_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/quarantine/items")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
