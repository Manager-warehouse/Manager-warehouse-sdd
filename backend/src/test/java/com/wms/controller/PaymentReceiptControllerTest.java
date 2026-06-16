package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.User;
import com.wms.enums.PaymentMethod;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.PaymentReceiptService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentReceiptController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class PaymentReceiptControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PaymentReceiptService paymentReceiptService;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private User accountant;

    @BeforeEach
    void setUp() {
        accountant = new User();
        accountant.setId(1L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);
        accountant.setFullName("Ke Toan");
    }

    @Test
    @DisplayName("POST /api/v1/payment-receipts — 201 Created khi ACCOUNTANT tạo phiếu thu")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createPaymentReceipt_accountant_returns201() throws Exception {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(5000000));
        request.setPaymentDate(LocalDate.of(2026, 6, 20));
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setNotes("Refund");

        PaymentReceiptResponse response = PaymentReceiptResponse.builder()
                .id(200L)
                .paymentNumber("PAY-202606-000200")
                .dealerId(10L)
                .invoiceId(50L)
                .amount(BigDecimal.valueOf(5000000))
                .paymentDate(LocalDate.of(2026, 6, 20))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(paymentReceiptService.createPaymentReceipt(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/payment-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment_number").value("PAY-202606-000200"))
                .andExpect(jsonPath("$.amount").value(5000000));
    }

    @Test
    @DisplayName("POST /api/v1/payment-receipts — 403 Forbidden khi STOREKEEPER tạo phiếu thu")
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void createPaymentReceipt_storekeeper_returns403() throws Exception {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(5000000));
        request.setPaymentDate(LocalDate.of(2026, 6, 20));
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        mockMvc.perform(post("/api/v1/payment-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/payment-receipts — 200 OK khi ACCOUNTANT truy cập")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getPaymentReceipts_accountant_returns200() throws Exception {
        List<PaymentReceiptResponse> list = List.of(
                PaymentReceiptResponse.builder().id(200L).paymentNumber("PAY-001").amount(BigDecimal.TEN).build()
        );
        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(paymentReceiptService.getPaymentReceipts(null, null, accountant)).thenReturn(list);

        mockMvc.perform(get("/api/v1/payment-receipts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].payment_number").value("PAY-001"));
    }
}
