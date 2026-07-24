package com.wms.controller;


import com.wms.controller.billing_payment.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.CorrectionVoucherCreateRequest;
import com.wms.dto.response.CorrectionVoucherResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.CorrectionVoucherService;
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

@WebMvcTest(CorrectionVoucherController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class CorrectionVoucherControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CorrectionVoucherService correctionVoucherService;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private User accountantManager;

    @BeforeEach
    void setUp() {
        accountantManager = new User();
        accountantManager.setId(6L);
        accountantManager.setEmail("acc_manager@wms.com");
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);
        accountantManager.setFullName("Ke Toan Truong");
    }

    private CorrectionVoucherCreateRequest sampleRequest() {
        CorrectionVoucherCreateRequest request = new CorrectionVoucherCreateRequest();
        request.setReferenceType(CorrectionVoucherReferenceType.INVOICE);
        request.setReferenceId(101L);
        request.setAmountDelta(BigDecimal.valueOf(-2000000));
        request.setReason("Hóa đơn ghi nhầm đơn giá, kỳ 2026-06 đã chốt sổ");
        request.setDocumentDate(LocalDate.of(2026, 7, 24));
        return request;
    }

    @Test
    @DisplayName("POST /api/v1/correction-vouchers — 201 Created khi ACCOUNTANT_MANAGER tạo bút toán")
    @WithMockUser(username = "acc_manager@wms.com", roles = "ACCOUNTANT_MANAGER")
    void createCorrectionVoucher_accountantManager_returns201() throws Exception {
        CorrectionVoucherResponse response = CorrectionVoucherResponse.builder()
                .id(5L)
                .adjustmentNumber("ADJ-20260724-ABC123")
                .referenceType(CorrectionVoucherReferenceType.INVOICE)
                .referenceId(101L)
                .dealerId(10L)
                .amountDelta(BigDecimal.valueOf(-2000000))
                .build();

        when(userRepository.findByEmail("acc_manager@wms.com")).thenReturn(Optional.of(accountantManager));
        when(correctionVoucherService.createCorrectionVoucher(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/correction-vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustment_number").value("ADJ-20260724-ABC123"))
                .andExpect(jsonPath("$.dealer_id").value(10))
                .andExpect(jsonPath("$.amount_delta").value(-2000000));
    }

    @Test
    @DisplayName("POST /api/v1/correction-vouchers — 403 Forbidden khi ACCOUNTANT (không phải Kế toán trưởng) gọi")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createCorrectionVoucher_accountant_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/correction-vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/correction-vouchers — 422 khi kỳ chứng từ gốc chưa CLOSED")
    @WithMockUser(username = "acc_manager@wms.com", roles = "ACCOUNTANT_MANAGER")
    void createCorrectionVoucher_originalPeriodNotClosed_returns422() throws Exception {
        when(userRepository.findByEmail("acc_manager@wms.com")).thenReturn(Optional.of(accountantManager));
        when(correctionVoucherService.createCorrectionVoucher(any(), any()))
                .thenThrow(new UnprocessableEntityException(
                        "ORIGINAL_PERIOD_NOT_CLOSED: Reference document's accounting period is not closed yet"));

        mockMvc.perform(post("/api/v1/correction-vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /api/v1/correction-vouchers — 404 khi chứng từ gốc không tồn tại")
    @WithMockUser(username = "acc_manager@wms.com", roles = "ACCOUNTANT_MANAGER")
    void createCorrectionVoucher_referenceNotFound_returns404() throws Exception {
        when(userRepository.findByEmail("acc_manager@wms.com")).thenReturn(Optional.of(accountantManager));
        when(correctionVoucherService.createCorrectionVoucher(any(), any()))
                .thenThrow(new ResourceNotFoundException("Invoice not found with id: 999"));

        mockMvc.perform(post("/api/v1/correction-vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/correction-vouchers — 200 OK khi ACCOUNTANT truy cập (chỉ xem)")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getCorrectionVouchers_accountant_returns200() throws Exception {
        User accountant = new User();
        accountant.setId(1L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);

        List<CorrectionVoucherResponse> list = List.of(
                CorrectionVoucherResponse.builder().id(5L).adjustmentNumber("ADJ-20260724-ABC123").build()
        );

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(correctionVoucherService.getCorrectionVouchers(null, accountant)).thenReturn(list);

        mockMvc.perform(get("/api/v1/correction-vouchers").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].adjustment_number").value("ADJ-20260724-ABC123"));
    }
}
