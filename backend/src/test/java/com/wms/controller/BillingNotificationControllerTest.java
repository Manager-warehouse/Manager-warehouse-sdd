package com.wms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.controller.notification_delivery.BillingNotificationController;
import com.wms.dto.response.BillingNotificationResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.BillingNotificationStatus;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.repository.UserRepository;
import com.wms.service.notification_delivery.BillingNotificationService;
import com.wms.util.JwtUtil;
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

@WebMvcTest(BillingNotificationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class BillingNotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private BillingNotificationService billingNotificationService;
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
    void getActiveNotifications_success() throws Exception {
        BillingNotificationResponse response = BillingNotificationResponse.builder()
                .id(100L)
                .dealerName("Dealer A")
                .status(BillingNotificationStatus.ACTIVE)
                .build();

        when(billingNotificationService.getActiveNotifications(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/billing-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].dealer_name").value("Dealer A"));
    }

    @Test
    @WithMockUser(username = "accountant@wms.com", roles = "ACCOUNTANT")
    void markAsRead_success() throws Exception {
        doNothing().when(billingNotificationService).markAsRead(eq(100L), any());

        mockMvc.perform(put("/api/v1/billing-notifications/100/read").with(csrf()))
                .andExpect(status().isOk());
    }
}
