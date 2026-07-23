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
import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.dto.response.RtvActionResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.RtvAlreadyExistsException;
import com.wms.repository.UserRepository;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.QuarantineRtvService;
import com.wms.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuarantineRtvController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ReceiptQuarantineControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private QuarantineRtvService quarantineRtvService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User manager;
    private User storekeeper;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(5L);
        manager.setEmail("manager@wms.com");
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setEmail("storekeeper@wms.com");
        storekeeper.setRole(UserRole.STOREKEEPER);
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createRtv_validRequest_returns201() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(quarantineRtvService.createRtv(eq(1L), any(ReceiptRtvCreateRequest.class), eq(manager)))
                .thenReturn(response(false));

        mockMvc.perform(post("/api/v1/receipts/1/rtv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":2,"reason":"Hàng lỗi QC"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confirmed").value(false))
                .andExpect(jsonPath("$.quarantineQty").value(20));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createRtv_missingReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/receipts/1/rtv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createRtv_duplicate_returns409() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(quarantineRtvService.createRtv(eq(1L), any(ReceiptRtvCreateRequest.class), eq(manager)))
                .thenThrow(new RtvAlreadyExistsException(1L));

        mockMvc.perform(post("/api/v1/receipts/1/rtv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":2,"reason":"Hàng lỗi QC"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RTV_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void confirmRtv_validRequest_returns200() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(quarantineRtvService.confirmRtv(eq(1L), any(ReceiptRtvConfirmRequest.class), eq(storekeeper)))
                .thenReturn(response(true));

        mockMvc.perform(put("/api/v1/receipts/1/rtv/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"returnedQty":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void confirmRtv_partialQuantity_returns422() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(quarantineRtvService.confirmRtv(eq(1L), any(ReceiptRtvConfirmRequest.class), eq(storekeeper)))
                .thenThrow(new BusinessRuleViolationException(
                        "RTV_QUANTITY_MISMATCH: Returned quantity 18 does not equal full quantity 20"));

        mockMvc.perform(put("/api/v1/receipts/1/rtv/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"returnedQty":18}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RTV_QUANTITY_MISMATCH"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void confirmRtv_staleVersion_returns409() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(quarantineRtvService.confirmRtv(eq(1L), any(ReceiptRtvConfirmRequest.class), eq(storekeeper)))
                .thenThrow(new BusinessRuleViolationException(
                        "INVENTORY_VERSION_CONFLICT: Receipt 1 has been modified"));

        mockMvc.perform(put("/api/v1/receipts/1/rtv/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":1,"returnedQty":20}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVENTORY_VERSION_CONFLICT"));
    }

    private RtvActionResponse response(boolean confirmed) {
        return RtvActionResponse.builder()
                .adjustmentId(100L)
                .adjustmentNumber("ADJ-20260613-ABCDEF")
                .debitNoteId(200L)
                .debitNoteNumber("DN-20260613-ABCDEF")
                .quarantineQty(BigDecimal.valueOf(20))
                .confirmed(confirmed)
                .confirmedAt(confirmed ? OffsetDateTime.now() : null)
                .message("OK")
                .build();
    }
}
