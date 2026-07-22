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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.DeliveryOtpResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.order_fulfillment.DeliveryOtpStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.order_fulfillment.DriverDeliveryService;
import com.wms.util.JwtUtil;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminDeliveryController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class AdminDeliveryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DriverDeliveryService driverDeliveryService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void resetDeliveryOtp_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(admin);
        when(driverDeliveryService.resetDeliveryOtp(eq(101L), any(), eq(admin)))
                .thenReturn(otpResponse());

        mockMvc.perform(post("/api/v1/admin/delivery-orders/101/delivery-otp/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetReason\":\"Dealer misread OTP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    @WithMockUser(username = "admin@wms.com", roles = "ADMIN")
    void resetDeliveryOtp_rejectsNonLockedRow() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(admin);
        when(driverDeliveryService.resetDeliveryOtp(eq(101L), any(), eq(admin)))
                .thenThrow(new OutboundDeliveryException("OTP_RESET_REQUIRED", HttpStatus.LOCKED, "Not locked"));

        mockMvc.perform(post("/api/v1/admin/delivery-orders/101/delivery-otp/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetReason\":\"Dealer misread OTP\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("OTP_RESET_REQUIRED"));
    }

    private DeliveryOtpResponse otpResponse() {
        return DeliveryOtpResponse.builder()
                .deliveryId(700L)
                .recipientEmail("dealer@example.com")
                .status(DeliveryOtpStatus.EXPIRED)
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .build();
    }
}

