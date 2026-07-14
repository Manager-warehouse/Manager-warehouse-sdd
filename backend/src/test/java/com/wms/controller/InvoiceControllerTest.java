package com.wms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.User;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.InvoiceService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class InvoiceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean InvoiceService invoiceService;
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
    @DisplayName("POST /api/v1/invoices — 201 Created khi ACCOUNTANT tạo hóa đơn")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void createInvoice_accountant_returns201() throws Exception {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(10L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));
        request.setNotes("Notes");

        InvoiceResponse response = InvoiceResponse.builder()
                .id(100L)
                .invoiceNumber("INV-202606-000100")
                .doId(10L)
                .totalAmount(BigDecimal.valueOf(5000000))
                .status(InvoiceStatus.UNPAID)
                .build();

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(invoiceService.createInvoice(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice_number").value("INV-202606-000100"))
                .andExpect(jsonPath("$.status").value("UNPAID"));
    }

    @Test
    @DisplayName("POST /api/v1/invoices — 403 Forbidden khi STOREKEEPER tạo hóa đơn")
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void createInvoice_storekeeper_returns403() throws Exception {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(10L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/invoices — 200 OK khi ACCOUNTANT truy cập")
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getInvoices_accountant_returns200() throws Exception {
        List<InvoiceResponse> list = List.of(
                InvoiceResponse.builder().id(100L).invoiceNumber("INV-001").totalAmount(BigDecimal.TEN).build()
        );
        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
        when(invoiceService.getInvoices(null, null, accountant)).thenReturn(list);

        mockMvc.perform(get("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].invoice_number").value("INV-001"));
    }
}
