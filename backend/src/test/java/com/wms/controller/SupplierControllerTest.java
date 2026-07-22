package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.SupplierReceivedOrderDetailResponse;
import com.wms.dto.response.SupplierReceivedOrderResponse;
import com.wms.dto.response.SupplierResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.CurrentUserService;
import com.wms.service.SupplierService;
import com.wms.util.JwtUtil;
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

@WebMvcTest(SupplierController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class SupplierControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SupplierService supplierService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountant;

    @BeforeEach
    void setUp() {
        accountant = new User();
        accountant.setId(2L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(accountant);
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getAllSuppliers_success() throws Exception {
        SupplierResponse s = SupplierResponse.builder()
                .id(1L)
                .code("SUP-001")
                .companyName("Cong ty Gia Dung Sunhouse")
                .isActive(true)
                .build();

        when(supplierService.getAllSuppliers()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SUP-001"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getSupplierById_success() throws Exception {
        SupplierResponse s = SupplierResponse.builder()
                .id(1L)
                .code("SUP-001")
                .companyName("Sunhouse")
                .build();

        when(supplierService.getSupplierById(1L)).thenReturn(s);

        mockMvc.perform(get("/api/v1/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Sunhouse"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createSupplier_success() throws Exception {
        SupplierResponse s = SupplierResponse.builder()
                .id(2L)
                .code("SUP-002")
                .companyName("LocknLock")
                .build();

        when(supplierService.createSupplier(any(), eq(accountant))).thenReturn(s);

        String json = "{\"code\":\"SUP-002\",\"companyName\":\"LocknLock\",\"phone\":\"0988776655\",\"email\":\"contact@locknlock.vn\"}";

        mockMvc.perform(post("/api/v1/suppliers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUP-002"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void deactivateSupplier_success() throws Exception {
        doNothing().when(supplierService).deactivateSupplier(eq(1L), eq(accountant));

        mockMvc.perform(delete("/api/v1/suppliers/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getReceivedOrders_success() throws Exception {
        SupplierReceivedOrderResponse order = SupplierReceivedOrderResponse.builder()
                .id(10L)
                .documentNumber("REC-2026-001")
                .build();

        when(supplierService.getReceivedOrders(1L)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/suppliers/1/received-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentNumber").value("REC-2026-001"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getReceivedOrderDetail_success() throws Exception {
        SupplierReceivedOrderDetailResponse detail = SupplierReceivedOrderDetailResponse.builder()
                .id(10L)
                .documentNumber("REC-2026-001")
                .contactPerson("Nguyen Van A")
                .build();

        when(supplierService.getReceivedOrderDetail(1L, 10L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/suppliers/1/received-orders/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("REC-2026-001"));
    }
}
