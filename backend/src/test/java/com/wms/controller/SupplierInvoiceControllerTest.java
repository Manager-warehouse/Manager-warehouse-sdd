package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.billing_payment.SupplierInvoiceController;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.SupplierInvoiceService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupplierInvoiceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class SupplierInvoiceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SupplierInvoiceService supplierInvoiceService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountant;

    @BeforeEach
    void setUp() {
        accountant = new User();
        accountant.setId(10L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createSupplierInvoice_success() throws Exception {
        SupplierInvoiceResponse response = SupplierInvoiceResponse.builder()
                .id(100L)
                .invoiceNumber("SINV-202607-000001")
                .supplierInvoiceNumber("VAT-NCC-001")
                .supplierName("Supplier Alpha")
                .totalAmount(new BigDecimal("50000000"))
                .status(InvoiceStatus.UNPAID)
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        when(supplierInvoiceService.createSupplierInvoice(any(), any())).thenReturn(response);

        String jsonBody = "{\"receiptId\":100,\"supplierInvoiceNumber\":\"VAT-NCC-001\",\"documentDate\":\"2026-07-23\"}";

        mockMvc.perform(post("/api/v1/supplier-invoices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.supplierInvoiceNumber").value("VAT-NCC-001"))
                .andExpect(jsonPath("$.status").value("UNPAID"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getSupplierInvoices_success() throws Exception {
        SupplierInvoiceResponse response = SupplierInvoiceResponse.builder()
                .id(100L)
                .invoiceNumber("SINV-202607-000001")
                .supplierInvoiceNumber("VAT-NCC-001")
                .status(InvoiceStatus.UNPAID)
                .build();

        when(supplierInvoiceService.getSupplierInvoices(eq(10L), eq("UNPAID"), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/supplier-invoices")
                        .param("supplierId", "10")
                        .param("status", "UNPAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("SINV-202607-000001"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getSupplierInvoiceById_success() throws Exception {
        SupplierInvoiceResponse response = SupplierInvoiceResponse.builder()
                .id(100L)
                .invoiceNumber("SINV-202607-000001")
                .status(InvoiceStatus.UNPAID)
                .build();

        when(supplierInvoiceService.getSupplierInvoiceById(eq(100L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/supplier-invoices/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }
}
