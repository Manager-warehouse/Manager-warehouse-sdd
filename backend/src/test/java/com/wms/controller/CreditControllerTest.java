package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.billing_payment.CreditController;
import com.wms.dto.response.CreditAgingReportResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.billing_payment.PaymentReceiptService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CreditController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class CreditControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PaymentReceiptService paymentReceiptService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountant;

    @BeforeEach
    void setUp() {
        accountant = new User();
        accountant.setId(20L);
        accountant.setEmail("accountant@wms.com");
        accountant.setRole(UserRole.ACCOUNTANT);

        when(userRepository.findByEmail("accountant@wms.com")).thenReturn(Optional.of(accountant));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void getCreditAgingReport_success() throws Exception {
        CreditAgingReportResponse response = CreditAgingReportResponse.builder()
                .dealerId(50L)
                .dealerName("Dealer HP")
                .currentBalance(new BigDecimal("150000000"))
                .overdue1To30(new BigDecimal("50000000"))
                .build();

        when(paymentReceiptService.getCreditAgingReport(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/credit/aging-report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dealer_id").value(50))
                .andExpect(jsonPath("$[0].dealer_name").value("Dealer HP"));
    }
}
