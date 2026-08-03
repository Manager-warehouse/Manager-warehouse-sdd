package com.wms.controller;

import com.wms.controller.billing_payment.*;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.PeriodSummaryResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.PeriodSummaryService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountingPeriodController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class PeriodSummaryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AccountingPeriodService accountingPeriodService;
    @MockBean PeriodSummaryService periodSummaryService;
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
        accountant.setFullName("Ke Toan Vien");
    }

    @Test
    @DisplayName("GET /api/v1/accounting-periods/{id}/summary — 200 OK khi ACCOUNTANT xem tong hop ky")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getPeriodSummary_accountant_returns200() throws Exception {
        PeriodSummaryResponse response = PeriodSummaryResponse.builder()
                .periodId(3L)
                .periodName("2026-07")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .status(AccountingPeriodStatus.OPEN)
                .invoiceCount(1)
                .invoiceTotal(BigDecimal.valueOf(17_000_000))
                .invoices(java.util.List.of())
                .payments(java.util.List.of())
                .supplierInvoices(java.util.List.of())
                .supplierPayments(java.util.List.of())
                .priceChanges(java.util.List.of())
                .cogs(BigDecimal.ZERO)
                .grossMargin(BigDecimal.valueOf(17_000_000))
                .build();

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(periodSummaryService.getPeriodSummary(3L, accountant)).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounting-periods/3/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period_name").value("2026-07"))
                .andExpect(jsonPath("$.invoice_total").value(17_000_000));
    }

    @Test
    @DisplayName("GET /api/v1/accounting-periods/{id}/summary — 404 khi ky khong ton tai")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getPeriodSummary_periodNotFound_returns404() throws Exception {
        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(periodSummaryService.getPeriodSummary(999L, accountant))
                .thenThrow(new ResourceNotFoundException("Accounting period not found with id: 999"));

        mockMvc.perform(get("/api/v1/accounting-periods/999/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounting-periods/{id}/summary — 403 khi role khong duoc phep (STOREKEEPER)")
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void getPeriodSummary_storekeeper_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/accounting-periods/3/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/accounting-periods/{id}/summary/export — 200 OK va tra ve file xlsx")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void exportPeriodSummary_accountant_returns200WithXlsxHeaders() throws Exception {
        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(periodSummaryService.exportPeriodSummaryXlsx(3L, accountant)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/accounting-periods/3/summary/export").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=ky-ke-toan-3.xlsx"));
    }
}
