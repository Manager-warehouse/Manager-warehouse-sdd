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
import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.request.ReceiptReturnConfirmRequest;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.ReceiptAlreadyDecidedException;
import com.wms.repository.UserRepository;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.ReceiptApprovalService;
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

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReceiptApprovalController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ReceiptControllerApprovalIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReceiptApprovalService receiptApprovalService;
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
    void approve_validRequest_returns200() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(receiptApprovalService.approveReceipt(eq(1L), any(ReceiptDecisionRequest.class), eq(manager)))
                .thenReturn(response(ReceiptStatus.APPROVED));

        mockMvc.perform(put("/api/v1/receipts/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"reason":"OK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void approve_missingExpectedVersion_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/receipts/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void reject_alreadyDecided_returns409() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(receiptApprovalService.rejectReceipt(eq(1L), any(ReceiptDecisionRequest.class), eq(manager)))
                .thenThrow(new ReceiptAlreadyDecidedException(1L, ReceiptStatus.APPROVED));

        mockMvc.perform(put("/api/v1/receipts/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"reason":"Lỗi QC"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECEIPT_ALREADY_DECIDED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void reject_forbiddenWarehouse_returns403() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(receiptApprovalService.rejectReceipt(eq(1L), any(ReceiptDecisionRequest.class), eq(manager)))
                .thenThrow(new ForbiddenReceiptWarehouseException(1L, 20L));

        mockMvc.perform(put("/api/v1/receipts/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"reason":"Lỗi QC"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_RECEIPT_WAREHOUSE"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void confirmReturn_validRequest_returns200() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(receiptApprovalService.confirmReturnToSupplier(eq(1L), any(ReceiptReturnConfirmRequest.class), eq(storekeeper)))
                .thenReturn(response(ReceiptStatus.RETURNED_TO_SUPPLIER));

        mockMvc.perform(put("/api/v1/receipts/1/return-to-supplier/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":4,"handoverNote":"Xe NCC 15A-12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED_TO_SUPPLIER"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void completePutaway_validRequest_returns200() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(receiptApprovalService.completePutaway(eq(1L), any(ReceiptPutawayRequest.class), eq(storekeeper)))
                .thenReturn(response(ReceiptStatus.APPROVED));

        mockMvc.perform(put("/api/v1/receipts/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":5,"items":[{"receiptItemId":11,"locationId":50}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"));
    }

    private ReceiptActionResponse response(ReceiptStatus status) {
        return ReceiptActionResponse.builder()
                .id(1L)
                .receiptNumber("RCV-2026-001")
                .status(status)
                .version(4)
                .updatedAt(OffsetDateTime.now())
                .message("OK")
                .build();
    }
}
