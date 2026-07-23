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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.dto.response.TransferPhotoUploadResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.warehouse_transfer.InterWarehouseTransferService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterWarehouseTransferController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class })
class InterWarehouseTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterWarehouseTransferService transferService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private com.wms.repository.UserRepository userRepository;

    @MockBean
    private com.wms.util.JwtUtil jwtUtil;

    @MockBean
    private com.wms.config.UserDetailsServiceImpl userDetailsService;

    private User manager;
    private User planner;
    private User dispatcher;
    private User storekeeper;
    private User driver;
    private User staff;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        planner = new User();
        planner.setId(2L);
        planner.setRole(UserRole.PLANNER);

        dispatcher = new User();
        dispatcher.setId(3L);
        dispatcher.setRole(UserRole.DISPATCHER);

        storekeeper = new User();
        storekeeper.setId(4L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        driver = new User();
        driver.setId(5L);
        driver.setRole(UserRole.DRIVER);

        staff = new User();
        staff.setId(6L);
        staff.setRole(UserRole.WAREHOUSE_STAFF);
    }

    private InterWarehouseTransferResponse createMockResponse(Long id, String number, InterWarehouseTransferStatus status) {
        return new InterWarehouseTransferResponse(
                id, number, "CTM-Instruction-01",
                10L, "HP-01", 20L, "HN-01",
                status, null, null, null, null, null, null, null,
                LocalDate.now(), LocalDate.now(), null, null, false, false,
                null, null, null, null, "Notes", false, OffsetDateTime.now(), OffsetDateTime.now(), List.of()
        );
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createTransfer_success() throws Exception {
        InterWarehouseTransferItemRequest item = new InterWarehouseTransferItemRequest(100L, 1L, 2L, new BigDecimal("5.00"));
        InterWarehouseTransferCreateRequest request = new InterWarehouseTransferCreateRequest(
                "CTM-0001", 10L, 20L, LocalDate.now(), LocalDate.now().plusDays(2), "Planner note", List.of(item)
        );

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.NEW);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(transferService.createTransfer(any(InterWarehouseTransferCreateRequest.class), eq(planner)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transferNumber").value("TRF-20260711-0001"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createTransfer_validationFailure_emptyCodeAndNegativeQty() throws Exception {
        // Empty instruction code, negative quantity
        InterWarehouseTransferItemRequest item = new InterWarehouseTransferItemRequest(100L, 1L, 2L, new BigDecimal("-1.00"));
        InterWarehouseTransferCreateRequest request = new InterWarehouseTransferCreateRequest(
                "", 10L, 20L, LocalDate.now(), LocalDate.now().plusDays(2), "Planner note", List.of(item)
        );

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void approveTransfer_success() throws Exception {
        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.APPROVED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(transferService.approveTransfer(eq(1L), eq(manager))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/approve")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void assignTrip_success() throws Exception {
        InterWarehouseTransferTripAssignRequest request = new InterWarehouseTransferTripAssignRequest(
                501L, 601L, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(4)
        );

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.APPROVED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(dispatcher);
        when(transferService.assignTrip(eq(1L), any(InterWarehouseTransferTripAssignRequest.class), eq(dispatcher)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/trip")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dispatcher@wms.com", roles = "DISPATCHER")
    void assignTrip_validationFailure_missingFields() throws Exception {
        // Missing fields (driverId null)
        InterWarehouseTransferTripAssignRequest request = new InterWarehouseTransferTripAssignRequest(
                501L, null, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(4)
        );

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/trip")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void shipTransfer_success() throws Exception {
        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.APPROVED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(transferService.shipTransfer(eq(1L), eq(storekeeper))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/ship")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void sourceLoadReport_success() throws Exception {
        SourceLoadReportRequest request = new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(101L, new BigDecimal("5.00"))), null);
        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.APPROVED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(staff);
        when(transferService.recordSourceLoadReport(eq(1L), any(SourceLoadReportRequest.class), eq(staff)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/source-load-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void sourceLoadReport_validationFailure_negativeQty() throws Exception {
        SourceLoadReportRequest request = new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(101L, new BigDecimal("-1.00"))), null);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/source-load-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void uploadPhotoEvidence_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "qc.jpg", "image/jpeg", "fake-image".getBytes());

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(transferService.uploadPhotoEvidence(eq(1L), any(), eq(storekeeper)))
                .thenReturn(new TransferPhotoUploadResponse("/uploads/transfer/trf-1-qc.jpg"));

        mockMvc.perform(multipart("/api/v1/inter-warehouse-transfers/1/photo-evidence")
                .file(file)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoRef").value("/uploads/transfer/trf-1-qc.jpg"));
    }

    @Test
    @WithMockUser(username = "driver@wms.com", roles = "DRIVER")
    void departTransfer_success() throws Exception {
        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.IN_TRANSIT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(driver);
        when(transferService.departTransfer(eq(1L), eq(driver))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/depart")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void receivingHandover_success() throws Exception {
        LoadHandoverRequest request = new LoadHandoverRequest("/uploads/transfer/arrival.jpg");
        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.IN_TRANSIT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(transferService.receivingHandover(eq(1L), any(LoadHandoverRequest.class), eq(storekeeper)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/receiving-handover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void receivingHandover_validationFailure_missingPhotoRef() throws Exception {
        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/receiving-handover")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"photoRef\":\"\"}")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void receiveCount_success() throws Exception {
        InterWarehouseTransferReceiveCountItemRequest item = new InterWarehouseTransferReceiveCountItemRequest(101L, new BigDecimal("5.00"), "");
        InterWarehouseTransferReceiveCountRequest request = new InterWarehouseTransferReceiveCountRequest(List.of(item));

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.IN_TRANSIT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(staff);
        when(transferService.receiveCount(eq(1L), any(InterWarehouseTransferReceiveCountRequest.class), eq(staff)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/inter-warehouse-transfers/1/receive-count")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void receiveCount_validationFailure_negativeQty() throws Exception {
        // Negative received quantity
        InterWarehouseTransferReceiveCountItemRequest item = new InterWarehouseTransferReceiveCountItemRequest(101L, new BigDecimal("-1.00"), "");
        InterWarehouseTransferReceiveCountRequest request = new InterWarehouseTransferReceiveCountRequest(List.of(item));

        mockMvc.perform(put("/api/v1/inter-warehouse-transfers/1/receive-count")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void receiveCheck_success() throws Exception {
        InterWarehouseTransferReceiveCheckItemRequest item = new InterWarehouseTransferReceiveCheckItemRequest(
                101L, new BigDecimal("5.00"), new BigDecimal("4.00"), new BigDecimal("1.00"), 12L, "check ok", "damaged"
        );
        InterWarehouseTransferReceiveCheckRequest request = new InterWarehouseTransferReceiveCheckRequest(
                List.of(item), "transfer/receive-qc/1.jpg");

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.IN_TRANSIT);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(transferService.receiveCheck(eq(1L), any(InterWarehouseTransferReceiveCheckRequest.class), eq(storekeeper)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/inter-warehouse-transfers/1/receive-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void receiveCheck_validationFailure_negativeQcPassed() throws Exception {
        // Negative qcPassedQty
        InterWarehouseTransferReceiveCheckItemRequest item = new InterWarehouseTransferReceiveCheckItemRequest(
                101L, new BigDecimal("5.00"), new BigDecimal("-1.00"), new BigDecimal("1.00"), 12L, "check ok", "damaged"
        );
        InterWarehouseTransferReceiveCheckRequest request = new InterWarehouseTransferReceiveCheckRequest(
                List.of(item), "transfer/receive-qc/1.jpg");

        mockMvc.perform(put("/api/v1/inter-warehouse-transfers/1/receive-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void finalReceive_success() throws Exception {
        InterWarehouseTransferFinalReceiveRequest request = new InterWarehouseTransferFinalReceiveRequest("Completed ok");

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.COMPLETED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(transferService.finalReceive(eq(1L), any(InterWarehouseTransferFinalReceiveRequest.class), eq(manager)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/final-receive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void quarantineReject_success() throws Exception {
        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("Damaged boxes");

        InterWarehouseTransferResponse response = createMockResponse(1L, "TRF-20260711-0001", InterWarehouseTransferStatus.QUARANTINED);

        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(transferService.quarantineReject(eq(1L), any(InterWarehouseTransferRejectRequest.class), eq(storekeeper)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inter-warehouse-transfers/1/quarantine-reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUARANTINED"));
    }
}
