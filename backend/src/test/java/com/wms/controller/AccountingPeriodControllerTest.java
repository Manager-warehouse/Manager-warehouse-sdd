package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.entity.User;
import com.wms.enums.AccountingPeriodStatus;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.AccountingPeriodService;
import com.wms.util.JwtUtil;
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

@WebMvcTest(AccountingPeriodController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class AccountingPeriodControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AccountingPeriodService accountingPeriodService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountantManager;

    @BeforeEach
    void setUp() {
        accountantManager = new User();
        accountantManager.setId(10L);
        accountantManager.setEmail("accountant_mgr@wms.com");
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);

        when(userRepository.findByEmail("accountant_mgr@wms.com")).thenReturn(Optional.of(accountantManager));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void getAllPeriods_success() throws Exception {
        AccountingPeriodResponse res = AccountingPeriodResponse.builder()
                .id(1L)
                .periodName("2026-07")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .status(AccountingPeriodStatus.OPEN)
                .build();

        when(accountingPeriodService.getAllPeriods(any())).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/accounting-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period_name").value("2026-07"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void createPeriod_success() throws Exception {
        AccountingPeriodResponse res = AccountingPeriodResponse.builder()
                .id(2L)
                .periodName("2026-08")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status(AccountingPeriodStatus.OPEN)
                .build();

        when(accountingPeriodService.createPeriod(any(), any())).thenReturn(res);

        String jsonBody = "{\"periodName\":\"2026-08\",\"notes\":\"New period\"}";

        mockMvc.perform(post("/api/v1/accounting-periods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.period_name").value("2026-08"));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void closePeriod_success() throws Exception {
        AccountingPeriodResponse res = AccountingPeriodResponse.builder()
                .id(1L)
                .periodName("2026-07")
                .status(AccountingPeriodStatus.CLOSED)
                .build();

        when(accountingPeriodService.closePeriod(eq(1L), any(), any())).thenReturn(res);

        mockMvc.perform(put("/api/v1/accounting-periods/1/close")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"End of month close\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
