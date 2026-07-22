package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.DealerResponse;
import com.wms.entity.User;
import com.wms.enums.CreditStatus;
import com.wms.enums.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.CurrentUserService;
import com.wms.service.DealerService;
import com.wms.util.JwtUtil;
import java.math.BigDecimal;
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

@WebMvcTest(DealerController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class DealerControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DealerService dealerService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User accountantMgr;

    @BeforeEach
    void setUp() {
        accountantMgr = new User();
        accountantMgr.setId(5L);
        accountantMgr.setEmail("accountant_mgr@wms.com");
        accountantMgr.setRole(UserRole.ACCOUNTANT_MANAGER);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(accountantMgr);
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void getAllDealers_success() throws Exception {
        DealerResponse res = DealerResponse.builder()
                .id(1L)
                .code("D001")
                .name("Dai ly Ha Noi")
                .creditLimit(new BigDecimal("500000000"))
                .creditStatus(CreditStatus.ACTIVE)
                .build();

        when(dealerService.getAllDealers()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/dealers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("D001"));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void getDealerById_success() throws Exception {
        DealerResponse res = DealerResponse.builder()
                .id(1L)
                .code("D001")
                .name("Dai ly Ha Noi")
                .build();

        when(dealerService.getDealerById(1L)).thenReturn(res);

        mockMvc.perform(get("/api/v1/dealers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("D001"));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void createDealer_success() throws Exception {
        DealerResponse res = DealerResponse.builder()
                .id(2L)
                .code("D002")
                .name("Dai ly Hai Phong")
                .build();

        when(dealerService.createDealer(any(), eq(accountantMgr))).thenReturn(res);

        String jsonBody = "{\"code\":\"D002\",\"name\":\"Dai ly Hai Phong\",\"phone\":\"0912345678\",\"email\":\"hp@dealer.com\",\"address\":\"Hai Phong\",\"creditLimit\":100000000,\"paymentTermDays\":30}";

        mockMvc.perform(post("/api/v1/dealers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("D002"));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void updateCreditLimit_success() throws Exception {
        DealerResponse res = DealerResponse.builder()
                .id(1L)
                .code("D001")
                .creditLimit(new BigDecimal("700000000"))
                .build();

        when(dealerService.updateCreditLimit(eq(1L), any(), eq(accountantMgr))).thenReturn(res);

        String jsonBody = "{\"creditLimit\":700000000,\"reason\":\"Upgrade quota\"}";

        mockMvc.perform(put("/api/v1/dealers/1/credit-limit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(700000000));
    }

    @Test
    @WithMockUser(username = "accountant_mgr@wms.com", roles = "ACCOUNTANT_MANAGER")
    void deactivateDealer_success() throws Exception {
        doNothing().when(dealerService).deactivateDealer(eq(1L), eq(accountantMgr));

        mockMvc.perform(delete("/api/v1/dealers/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
