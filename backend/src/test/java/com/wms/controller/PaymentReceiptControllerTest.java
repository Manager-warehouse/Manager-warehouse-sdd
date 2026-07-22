package com.wms.controller;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.controller.user_configuration.*;
import com.wms.controller.audit_trail.*;
import com.wms.controller.access_control.*;
import com.wms.controller.billing_payment.*;
import com.wms.controller.stock_receiving.*;
import com.wms.controller.stock_control.*;
import com.wms.controller.notification_delivery.*;
import com.wms.controller.order_fulfillment.*;
import com.wms.controller.price_management.*;
import com.wms.controller.reporting_alerting.*;
import com.wms.controller.return_disposal.*;
import com.wms.controller.stock_counting.*;
import com.wms.controller.fleet_management.*;
import com.wms.controller.warehouse_location.*;
import com.wms.controller.warehouse_transfer.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.billing_payment.PaymentMethod;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.PaymentReceiptService;
import com.wms.util.JwtUtil;
import com.wms.dto.response.PaymentReceiptOcrResponse;
import com.wms.service.billing_payment.OcrService;
import org.springframework.mock.web.MockMultipartFile;
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
    @MockBean OcrService ocrService;

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

    @Test
    @DisplayName("POST /api/v1/payment-receipts/ocr — 200 OK khi upload ảnh hóa đơn hợp lệ")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void scanPaymentReceipt_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt_25000000_hoang_phat.png", "image/png", "fake image content".getBytes()
        );

        PaymentReceiptOcrResponse response = PaymentReceiptOcrResponse.builder()
                .amount(BigDecimal.valueOf(25000000))
                .paymentDate(LocalDate.of(2026, 6, 20))
                .dealerId(1L)
                .notes("CK TIEN HANG")
                .confidenceScore(0.95)
                .build();

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(ocrService.processOcr(any(), any())).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/payment-receipts/ocr")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25000000))
                .andExpect(jsonPath("$.dealer_id").value(1))
                .andExpect(jsonPath("$.confidence_score").value(0.95));
    }

    @Test
    @DisplayName("POST /api/v1/payment-receipts/ocr — 400 Bad Request khi upload sai định dạng file")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void scanPaymentReceipt_invalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.txt", "text/plain", "fake text content".getBytes()
        );

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/payment-receipts/ocr")
                        .file(file))
                .andExpect(status().isBadRequest());
    }
}
