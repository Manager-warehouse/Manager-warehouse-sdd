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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.dto.response.ReceiptItemResponse;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ReceiptCountException;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.stock_receiving.ReceiptService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.wms.config.JacksonConfig;
import com.wms.config.SecurityConfig;
import com.wms.config.JwtAuthFilter;
import com.wms.exception.GlobalExceptionHandler;

@WebMvcTest(ReceiptController.class)
@Import({ SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class, JacksonConfig.class })
class ReceiptControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
    private ReceiptService receiptService;

        @MockBean
    private CurrentUserService currentUserService;

        @MockBean
    private com.wms.service.stock_receiving.ReceiptQcService receiptQcService;

        @MockBean
    private com.wms.repository.UserRepository userRepository;

        @MockBean
    private com.wms.util.JwtUtil jwtUtil;

        @MockBean
    private com.wms.config.UserDetailsServiceImpl userDetailsService;

        private User planner;
        private User warehouseStaff;

        @BeforeEach
        void setUp() {
                planner = new User();
                planner.setId(1L);
                planner.setRole(UserRole.PLANNER);
                warehouseStaff = new User();
                warehouseStaff.setId(2L);
                warehouseStaff.setRole(UserRole.WAREHOUSE_STAFF);
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_success() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
                when(receiptService.createPurchaseReceipt(any(), any())).thenReturn(response());

                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson()))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.receipt_number").value("RN-20260613-0001"))
                                .andExpect(jsonPath("$.type").value("PURCHASE"))
                                .andExpect(jsonPath("$.status").value("PENDING_RECEIPT"))
                                .andExpect(jsonPath("$.items[0].expected_qty").value(500));
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_rejectsMissingMandatoryFields() throws Exception {
                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contact_person\":\"A\",\"items\":[]}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_rejectsInvalidChannel() throws Exception {
                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson().replace("ZALO", "PHONE")))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_rejectsFractionalExpectedQty() throws Exception {
                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson().replace("500", "1.5")))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_rejectsDuplicateSourceReference() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
                when(receiptService.createPurchaseReceipt(any(), any()))
                                .thenThrow(new DuplicateResourceException("duplicate"));

                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson()))
                                .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void createReceipt_rejectsReturnFlowField() throws Exception {
                String json = validJson().replace("\"supplier_id\"", "\"type\":\"RETURN\",\"supplier_id\"");

                mockMvc.perform(post("/api/v1/receipts")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_success() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
                when(receiptService.receiveReceiptCounts(eq(100L), any(), eq(warehouseStaff)))
                                .thenReturn(receivedResponse());

                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DRAFT"))
                                .andExpect(jsonPath("$.items[0].receipt_item_id").value(501))
                                .andExpect(jsonPath("$.items[0].actual_qty").value(90))
                                .andExpect(jsonPath("$.items[1].over_received_qty").value(20));
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_correctsAfterQcDataExists() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
                when(receiptService.receiveReceiptCounts(eq(100L), any(), eq(warehouseStaff)))
                                .thenReturn(receivedResponse());

                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DRAFT"))
                                .andExpect(jsonPath("$.items[0].actual_qty").value(90));
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_rejectsInvalidPayload() throws Exception {
                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"items\":[]}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_rejectsFractionalCount() throws Exception {
                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson().replace("90", "90.5")))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
        void receiveReceipt_returnsForbiddenForWrongRole() throws Exception {
                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_returnsConflictForFinalizedReceipt() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
                when(receiptService.receiveReceiptCounts(eq(100L), any(), eq(warehouseStaff)))
                                .thenThrow(new ReceiptCountException("RECEIPT_ALREADY_FINALIZED",
                                                HttpStatus.CONFLICT, "finalized"));

                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson()))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.code").value("RECEIPT_ALREADY_FINALIZED"));
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void receiveReceipt_returnsUnprocessableForInvalidCount() throws Exception {
                when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
                when(receiptService.receiveReceiptCounts(eq(100L), any(), eq(warehouseStaff)))
                                .thenThrow(new ReceiptCountException("INVALID_RECEIPT_COUNT",
                                                HttpStatus.UNPROCESSABLE_ENTITY, "invalid"));

                mockMvc.perform(put("/api/v1/receipts/100/receive")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiveJson()))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.code").value("INVALID_RECEIPT_COUNT"));
        }

        @Test
        @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
        void processQc_submit_success() throws Exception {
                ReceiptQcResponse res = ReceiptQcResponse.builder()
                                .receiptId(100L)
                                .receiptNumber("RCV-001")
                                .items(java.util.Collections.emptyList())
                                .build();

                when(receiptQcService.processQc(eq(100L), any(), eq("staff@wms.com"))).thenReturn(res);

                mockMvc.perform(put("/api/v1/receipts/100/qc")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\":\"SUBMIT\",\"items\":[{\"receipt_item_id\":501,\"qc_passed_qty\":90,\"qc_failed_qty\":5}]}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.receipt_id").value(100))
                                .andExpect(jsonPath("$.receipt_number").value("RCV-001"));
        }

        @Test
        @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
        void processQc_confirm_success() throws Exception {
                ReceiptQcResponse res = ReceiptQcResponse.builder()
                                .receiptId(100L)
                                .receiptNumber("RCV-001")
                                .items(java.util.Collections.emptyList())
                                .build();

                when(receiptQcService.processQc(eq(100L), any(), eq("storekeeper@wms.com"))).thenReturn(res);

                mockMvc.perform(put("/api/v1/receipts/100/qc")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\":\"CONFIRM\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.receipt_id").value(100));
        }

        private ReceiptResponse response() {
                ReceiptItemResponse item = new ReceiptItemResponse();
                item.setReceiptItemId(501L);
                item.setProductId(30L);
                item.setExpectedQty(500);

                ReceiptResponse response = new ReceiptResponse();
                response.setId(100L);
                response.setReceiptNumber("RN-20260613-0001");
                response.setType("PURCHASE");
                response.setStatus("PENDING_RECEIPT");
                response.setSupplierId(10L);
                response.setWarehouseId(20L);
                response.setSourceReference("PO-1");
                response.setSourceChannel("ZALO");
                response.setDocumentDate(LocalDate.of(2026, 6, 13));
                response.setItems(List.of(item));
                return response;
        }

        private ReceiptResponse receivedResponse() {
                ReceiptItemResponse item1 = new ReceiptItemResponse();
                item1.setReceiptItemId(501L);
                item1.setProductId(30L);
                item1.setExpectedQty(100);
                item1.setActualQty(90);
                item1.setOverReceivedQty(0);

                ReceiptItemResponse item2 = new ReceiptItemResponse();
                item2.setReceiptItemId(502L);
                item2.setProductId(31L);
                item2.setExpectedQty(100);
                item2.setActualQty(100);
                item2.setOverReceivedQty(20);

                ReceiptResponse response = response();
                response.setStatus("DRAFT");
                response.setItems(List.of(item1, item2));
                return response;
        }

        private String validJson() {
                return """
                                {
                                  "supplier_id": 10,
                                  "contact_person": "Nguyen Van A",
                                  "warehouse_id": 20,
                                  "source_reference": "PO-1",
                                  "source_channel": "ZALO",
                                  "items": [
                                    {
                                      "product_id": 30,
                                      "expected_qty": 500
                                    }
                                  ]
                                }
                                """;
        }

        private String receiveJson() {
                return """
                                {
                                  "items": [
                                    {
                                      "receipt_item_id": 501,
                                      "counted_qty": 90
                                    },
                                    {
                                      "receipt_item_id": 502,
                                      "counted_qty": 120
                                    }
                                  ]
                                }
                                """;
        }
}


