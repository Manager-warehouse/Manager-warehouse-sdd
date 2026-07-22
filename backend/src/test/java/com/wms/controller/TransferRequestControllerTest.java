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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.dto.request.TransferRequestCreateRequest;
import com.wms.dto.request.TransferRequestItemRequest;
import com.wms.dto.request.TransferRequestRejectRequest;
import com.wms.dto.response.TransferRequestResponse;
import com.wms.dto.response.WarehouseStockLookupResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.warehouse_transfer.TransferRequestStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.warehouse_transfer.TransferRequestService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

@WebMvcTest(TransferRequestController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class TransferRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferRequestService requestService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private com.wms.repository.UserRepository userRepository;

    @MockBean
    private com.wms.util.JwtUtil jwtUtil;

    @MockBean
    private com.wms.config.UserDetailsServiceImpl userDetailsService;

    private User manager;
    private User ceo;
    private User planner;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        ceo = new User();
        ceo.setId(2L);
        ceo.setRole(UserRole.CEO);

        planner = new User();
        planner.setId(3L);
        planner.setRole(UserRole.PLANNER);
    }

    private TransferRequestResponse createMockResponse(Long id, String number, TransferRequestStatus status) {
        return new TransferRequestResponse(
                id, number, 10L, "Source Warehouse", 20L, "Destination Warehouse", status,
                1L, "Creator Name", null, null, null, null, null, null, null, null, null,
                null, LocalDate.now().plusDays(2), "Destination shortage", "Notes detail",
                null, null, null, null, null, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        );
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void createRequest_success() throws Exception {
        TransferRequestItemRequest item = new TransferRequestItemRequest(100L, new BigDecimal("10.00"));

        TransferRequestCreateRequest request = new TransferRequestCreateRequest(
                10L, 20L, LocalDate.now().plusDays(2), "Destination shortage", "Notes details", List.of(item)
        );

        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.DRAFT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(requestService.createRequest(any(TransferRequestCreateRequest.class), eq(manager)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.requestNumber").value("TRQ-0001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void submitRequest_success() throws Exception {
        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.SUBMITTED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(requestService.submitRequest(eq(500L), eq(manager)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests/500/submit")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void cancelRequest_success() throws Exception {
        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.CANCELLED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(requestService.cancelRequest(eq(500L), eq(manager)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests/500/cancel")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void approveRequest_success() throws Exception {
        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.APPROVED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(ceo);
        when(requestService.approveRequest(eq(500L), eq(ceo)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests/500/approve")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "ceo@wms.com", roles = "CEO")
    void rejectRequest_success() throws Exception {
        TransferRequestRejectRequest rejectReq = new TransferRequestRejectRequest("Out of budget");

        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.REJECTED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(ceo);
        when(requestService.rejectRequest(eq(500L), any(TransferRequestRejectRequest.class), eq(ceo)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests/500/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rejectReq))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void convertToTransfer_success() throws Exception {
        TransferRequestResponse response = createMockResponse(500L, "TRQ-0001", TransferRequestStatus.CONVERTED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(requestService.convertToTransfer(eq(500L), eq(planner)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transfer-requests/500/convert")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVERTED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void stockLookup_success() throws Exception {
        WarehouseStockLookupResponse stock = new WarehouseStockLookupResponse(10L, "HP-01", new BigDecimal("15.00"));

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(requestService.stockLookup(eq(100L), eq(manager)))
                .thenReturn(List.of(stock));

        mockMvc.perform(get("/api/v1/transfer-requests/stock-lookup")
                .param("productId", "100")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseId").value(10))
                .andExpect(jsonPath("$[0].availableQty").value(15.00));
    }
}
