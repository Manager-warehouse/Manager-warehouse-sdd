package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.price_management.PriceHistoryController;
import com.wms.controller.price_management.ProductPriceHistoryController;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.price_management.PriceHistoryService;
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

@WebMvcTest({PriceHistoryController.class, ProductPriceHistoryController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class PriceHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PriceHistoryService priceHistoryService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountant;

    @BeforeEach
    void setUp() {
        accountant = new User();
        accountant.setId(7L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(accountant);
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createPriceHistory_success() throws Exception {
        PriceHistoryResponse res = PriceHistoryResponse.builder()
                .id(10L)
                .productId(100L)
                .productSku("NOI-001")
                .costPrice(new BigDecimal("150000"))
                .sellingPrice(new BigDecimal("200000"))
                .status("PENDING")
                .build();

        when(priceHistoryService.create(any(), eq(accountant))).thenReturn(res);

        String json = "{\"product_id\":100,\"warehouse_id\":1,\"cost_price\":150000,\"selling_price\":200000,\"effective_date\":\"2026-08-01\"}";

        mockMvc.perform(post("/api/v1/price-history")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.product_sku").value("NOI-001"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT_MANAGER")
    void approvePriceHistory_success() throws Exception {
        PriceHistoryResponse res = PriceHistoryResponse.builder()
                .id(10L)
                .status("APPROVED")
                .build();

        when(priceHistoryService.approve(eq(10L), any())).thenReturn(res);

        mockMvc.perform(put("/api/v1/price-history/10/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void downloadTemplate_success() throws Exception {
        mockMvc.perform(get("/api/v1/price-history/import/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=price_import_template.xlsx"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getProductPriceHistory_success() throws Exception {
        ProductPriceHistoryResponse res = ProductPriceHistoryResponse.builder()
                .productId(100L)
                .productSku("NOI-001")
                .entries(List.of())
                .build();

        when(priceHistoryService.getByProduct(100L)).thenReturn(res);

        mockMvc.perform(get("/api/v1/products/100/price-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSku").value("NOI-001"));
    }
}
