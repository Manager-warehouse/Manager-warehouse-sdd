package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.billing_payment.SupplierPaymentController;
import com.wms.dto.response.SupplierPaymentOcrResponse;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.PaymentMethod;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.SupplierPaymentService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupplierPaymentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class SupplierPaymentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SupplierPaymentService supplierPaymentService;
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
    void createSupplierPayment_success() throws Exception {
        SupplierPaymentResponse response = SupplierPaymentResponse.builder()
                .id(200L)
                .paymentNumber("SPAY-202607-000001")
                .amount(new BigDecimal("20000000"))
                .paymentDate(LocalDate.of(2026, 7, 23))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        when(supplierPaymentService.createSupplierPayment(any(), any())).thenReturn(response);

        String jsonBody = "{\"supplierId\":10,\"supplierInvoiceId\":50,\"amount\":20000000,\"paymentDate\":\"2026-07-23\",\"paymentMethod\":\"BANK_TRANSFER\",\"documentDate\":\"2026-07-23\"}";

        mockMvc.perform(post("/api/v1/supplier-payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.paymentNumber").value("SPAY-202607-000001"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getSupplierPayments_success() throws Exception {
        SupplierPaymentResponse response = SupplierPaymentResponse.builder()
                .id(200L)
                .paymentNumber("SPAY-202607-000001")
                .amount(new BigDecimal("20000000"))
                .build();

        when(supplierPaymentService.getSupplierPayments(eq(10L), eq(50L), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/supplier-payments")
                        .param("supplierId", "10")
                        .param("invoiceId", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void scanSupplierPaymentOcr_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "unc.jpg", "image/jpeg", "content".getBytes());

        SupplierPaymentOcrResponse response = SupplierPaymentOcrResponse.builder()
                .amount(new BigDecimal("25000000"))
                .supplierId(10L)
                .confidenceScore(0.92)
                .build();

        when(supplierPaymentService.scanSupplierPaymentOcr(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/supplier-payments/ocr")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25000000))
                .andExpect(jsonPath("$.supplierId").value(10));
    }
}
