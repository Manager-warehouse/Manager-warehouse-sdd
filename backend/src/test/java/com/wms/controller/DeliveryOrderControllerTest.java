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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wms.config.JwtAuthFilter;
import com.wms.config.SecurityConfig;
import com.wms.config.UserDetailsServiceImpl;
import com.wms.dto.response.DeliveryOrderAllocationResponse;
import com.wms.dto.response.DeliveryOrderItemResponse;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.dto.response.ReturnedGoodsFlowItemResponse;
import com.wms.dto.response.ReturnedGoodsFlowResponse;
import com.wms.entity.access_control.User;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderType;
import com.wms.enums.order_fulfillment.ReturnedDeliveryFlowStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.GlobalExceptionHandler;
import com.wms.exception.OutboundDeliveryException;
import com.wms.service.user_context.CurrentUserService;
import com.wms.service.order_fulfillment.DeliveryOrderService;
import com.wms.util.JwtUtil;
import java.time.LocalDate;
import java.util.List;
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

@WebMvcTest(DeliveryOrderController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class DeliveryOrderControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private DeliveryOrderService deliveryOrderService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsServiceImpl userDetailsService;

    private User planner;
    private User manager;
    private User storekeeper;
    private User warehouseStaff;

    @BeforeEach
    void setUp() {
        planner = user(1L, UserRole.PLANNER);
        manager = user(2L, UserRole.WAREHOUSE_MANAGER);
        storekeeper = user(3L, UserRole.STOREKEEPER);
        warehouseStaff = user(4L, UserRole.WAREHOUSE_STAFF);
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createDeliveryOrder_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(deliveryOrderService.createDeliveryOrder(any(), eq(planner))).thenReturn(response(DeliveryOrderStatus.NEW));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.dealerId").value(10))
                .andExpect(jsonPath("$.warehouseId").value(20));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void getAllDeliveryOrders_allowsStorekeeper() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.getAllDeliveryOrders(storekeeper))
                .thenReturn(List.of(response(DeliveryOrderStatus.NEW)));

        mockMvc.perform(get("/api/v1/delivery-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void getDeliveryOrderById_allowsStorekeeper() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.getDeliveryOrderById(100L, storekeeper))
                .thenReturn(response(DeliveryOrderStatus.WAITING_PICKING));

        mockMvc.perform(get("/api/v1/delivery-orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PICKING"));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void createDeliveryOrder_rejectsBusinessError() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(planner);
        when(deliveryOrderService.createDeliveryOrder(any(), eq(planner)))
                .thenThrow(new OutboundDeliveryException("INSUFFICIENT_STOCK",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient stock"));

        mockMvc.perform(post("/api/v1/delivery-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void cancelDeliveryOrder_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(deliveryOrderService.cancelDeliveryOrder(eq(100L), any(), eq(manager)))
                .thenReturn(response(DeliveryOrderStatus.CANCELLED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Customer changed order\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "planner@wms.com", roles = "PLANNER")
    void cancelDeliveryOrder_rejectsNonManagerRole() throws Exception {
        mockMvc.perform(put("/api/v1/delivery-orders/100/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"Customer changed order\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void savePickingPlan_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.saveDeliveryOrderPickingPlan(eq(100L), any(), eq(storekeeper)))
                .thenReturn(response(DeliveryOrderStatus.WAITING_PICKING));

        mockMvc.perform(put("/api/v1/delivery-orders/100/picking-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickingPlanJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PICKING"))
                .andExpect(jsonPath("$.items[0].plannedQty").value(10))
                .andExpect(jsonPath("$.items[0].allocations[0].inventoryId").value(501));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void savePickingPlan_rejectsBusinessError() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.saveDeliveryOrderPickingPlan(eq(100L), any(), eq(storekeeper)))
                .thenThrow(new OutboundDeliveryException("PICKING_PLAN_QTY_MISMATCH",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Planned quantity mismatch"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/picking-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickingPlanJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PICKING_PLAN_QTY_MISMATCH"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void savePickingPlan_rejectsMissingReturnToBinRecord() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.saveDeliveryOrderPickingPlan(eq(100L), any(), eq(storekeeper)))
                .thenThrow(new OutboundDeliveryException("PICKED_GOODS_RETURN_REQUIRED",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Changing a picked allocation requires return-to-bin records"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/picking-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revisionPickingPlanJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PICKED_GOODS_RETURN_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void saveReplacementPlan_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.saveDeliveryOrderReplacementPlan(eq(100L), any(), eq(storekeeper)))
                .thenReturn(response(DeliveryOrderStatus.WAITING_PICKING));

        mockMvc.perform(put("/api/v1/delivery-orders/100/replacement-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replacementPlanJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PICKING"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void saveReplacementPlan_rejectsInvalidStatus() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.saveDeliveryOrderReplacementPlan(eq(100L), any(), eq(storekeeper)))
                .thenThrow(new OutboundDeliveryException("DELIVERY_ORDER_STATUS_INVALID",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Replacement plan can only be saved from QC_PENDING_APPROVAL status"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/replacement-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(replacementPlanJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DELIVERY_ORDER_STATUS_INVALID"));
    }

    @Test
    @WithMockUser(username = "warehouse@wms.com", roles = "WAREHOUSE_STAFF")
    void savePickQcResult_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.saveDeliveryOrderPickQcResult(eq(100L), any(), eq(warehouseStaff)))
                .thenReturn(response(DeliveryOrderStatus.QC_PENDING_APPROVAL));

        mockMvc.perform(put("/api/v1/delivery-orders/100/pick-qc-result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickQcResultJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QC_PENDING_APPROVAL"));
    }

    @Test
    @WithMockUser(username = "warehouse@wms.com", roles = "WAREHOUSE_STAFF")
    void savePickQcResult_replaysSamePayload() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.saveDeliveryOrderPickQcResult(eq(100L), any(), eq(warehouseStaff)))
                .thenReturn(response(DeliveryOrderStatus.QC_PENDING_APPROVAL));

        mockMvc.perform(put("/api/v1/delivery-orders/100/pick-qc-result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickQcResultJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QC_PENDING_APPROVAL"));
    }

    @Test
    @WithMockUser(username = "warehouse@wms.com", roles = "WAREHOUSE_STAFF")
    void savePickQcResult_rejectsDuplicateSubmission() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.saveDeliveryOrderPickQcResult(eq(100L), any(), eq(warehouseStaff)))
                .thenThrow(new OutboundDeliveryException("QC_RESULT_ALREADY_RECORDED",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Pick/QC result already recorded"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/pick-qc-result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickQcResultJson()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QC_RESULT_ALREADY_RECORDED"));
    }

    @Test
    @WithMockUser(username = "warehouse@wms.com", roles = "WAREHOUSE_STAFF")
    void savePickQcResult_rejectsIdempotencyConflict() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.saveDeliveryOrderPickQcResult(eq(100L), any(), eq(warehouseStaff)))
                .thenThrow(new OutboundDeliveryException("IDEMPOTENCY_KEY_CONFLICT",
                        HttpStatus.CONFLICT, "Idempotency key conflict"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/pick-qc-result")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickQcResultJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void approveQuality_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.approveDeliveryOrderQuality(eq(100L), any(), eq(storekeeper)))
                .thenReturn(response(DeliveryOrderStatus.QC_COMPLETED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/quality-approval")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"All replacement goods passed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QC_COMPLETED"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void approveQuality_rejectsReplacementRequired() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.approveDeliveryOrderQuality(eq(100L), any(), eq(storekeeper)))
                .thenThrow(new OutboundDeliveryException("QC_REPLACEMENT_REQUIRED",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Replacement required"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/quality-approval")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Try approval early\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QC_REPLACEMENT_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void warehouseApproval_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(deliveryOrderService.approveDeliveryOrderWarehouseRelease(eq(100L), any(), eq(manager)))
                .thenReturn(response(DeliveryOrderStatus.WAREHOUSE_APPROVED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/warehouse-approval")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Release approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAREHOUSE_APPROVED"));
    }

    @Test
    @WithMockUser(username = "manager@wms.com", roles = "WAREHOUSE_MANAGER")
    void warehouseReject_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(manager);
        when(deliveryOrderService.rejectDeliveryOrderWarehouseRelease(eq(100L), any(), eq(manager)))
                .thenReturn(response(DeliveryOrderStatus.REJECTED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/warehouse-reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(warehouseRejectJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void submitReturnedGoodsCountQc_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.submitReturnedGoodsCountQc(eq(100L), any(), eq(warehouseStaff)))
                .thenReturn(returnedFlowResponse(ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                        DeliveryOrderStatus.RETURNED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/returned-goods/count-qc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnedCountQcJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowStatus").value("COUNT_QC_SUBMITTED"))
                .andExpect(jsonPath("$.items[0].qualityPassQty").value(8));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void confirmReturnedGoodsReceived_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.confirmReturnedGoodsReceived(eq(100L), any(), eq(storekeeper)))
                .thenReturn(returnedFlowResponse(ReturnedDeliveryFlowStatus.COUNT_QC_PENDING,
                        DeliveryOrderStatus.RETURNED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/returned-goods/receive")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Returned goods arrived\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowStatus").value("COUNT_QC_PENDING"));
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void getReturnedGoodsFlow_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.getReturnedGoodsFlow(100L, warehouseStaff))
                .thenReturn(returnedFlowResponse(ReturnedDeliveryFlowStatus.COUNT_QC_SUBMITTED,
                        DeliveryOrderStatus.RETURNED));

        mockMvc.perform(get("/api/v1/delivery-orders/100/returned-goods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryOrderStatus").value("RETURNED"))
                .andExpect(jsonPath("$.flowStatus").value("COUNT_QC_SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void approveReturnedGoods_rejectsStateError() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.approveReturnedGoods(eq(100L), any(), eq(storekeeper)))
                .thenThrow(new OutboundDeliveryException("RETURN_QTY_MISMATCH",
                        HttpStatus.UNPROCESSABLE_ENTITY, "Returned quantity mismatch"));

        mockMvc.perform(put("/api/v1/delivery-orders/100/returned-goods/approval")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"ACCEPT\",\"notes\":\"Approve returned goods\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RETURN_QTY_MISMATCH"));
    }

    @Test
    @WithMockUser(username = "storekeeper@wms.com", roles = "STOREKEEPER")
    void planReturnedGoodsPutaway_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(storekeeper);
        when(deliveryOrderService.planReturnedGoodsPutaway(eq(100L), any(), eq(storekeeper)))
                .thenReturn(returnedFlowResponse(ReturnedDeliveryFlowStatus.PUTAWAY_PLANNED,
                        DeliveryOrderStatus.RETURNED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/returned-goods/putaway-plan")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnedPutawayPlanJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flowStatus").value("PUTAWAY_PLANNED"));
    }

    @Test
    @WithMockUser(username = "staff@wms.com", roles = "WAREHOUSE_STAFF")
    void completeReturnedGoodsPutaway_success() throws Exception {
        when(currentUserService.getRequiredCurrentUser()).thenReturn(warehouseStaff);
        when(deliveryOrderService.completeReturnedGoodsPutaway(eq(100L), any(), eq(warehouseStaff)))
                .thenReturn(returnedFlowResponse(ReturnedDeliveryFlowStatus.PUTAWAY_COMPLETED,
                        DeliveryOrderStatus.DELIVERY_FAILED));

        mockMvc.perform(put("/api/v1/delivery-orders/100/returned-goods/putaway-complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Putaway completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryOrderStatus").value("DELIVERY_FAILED"))
                .andExpect(jsonPath("$.flowStatus").value("PUTAWAY_COMPLETED"));
    }

    private String createJson() {
        return """
                {
                  "dealerId": 10,
                  "warehouseId": 20,
                  "type": "SALE",
                  "documentDate": "2026-06-18",
                  "expectedDeliveryDate": "2026-06-20",
                  "items": [
                    {"productId": 30, "requestedQty": 10}
                  ]
                }
                """;
    }

    private String pickingPlanJson() {
        return """
                {
                  "allocations": [
                    {
                      "doItemId": 200,
                      "inventoryId": 501,
                      "batchId": 71,
                      "locationId": 801,
                      "zoneId": 31,
                      "plannedQty": 10
                    }
                  ]
                }
                """;
    }

    private String revisionPickingPlanJson() {
        return """
                {
                  "allocations": [
                    {
                      "doItemId": 200,
                      "inventoryId": 501,
                      "batchId": 71,
                      "locationId": 801,
                      "zoneId": 31,
                      "plannedQty": 6
                    },
                    {
                      "doItemId": 200,
                      "inventoryId": 502,
                      "batchId": 72,
                      "locationId": 802,
                      "zoneId": 32,
                      "plannedQty": 4
                    }
                  ],
                  "returnToBinRecords": [
                    {
                      "allocationId": 900,
                      "returnedQty": 4,
                      "sourceLocationId": 880,
                      "reason": "Rebalance after picked bin became blocked"
                    }
                  ]
                }
                """;
    }

    private String replacementPlanJson() {
        return """
                {
                  "replacements": [
                    {
                      "doItemId": 200,
                      "failedInventoryId": 501,
                      "failedBatchId": 71,
                      "failedLocationId": 801,
                      "replacementInventoryId": 502,
                      "replacementBatchId": 72,
                      "replacementLocationId": 802,
                      "replacementZoneId": 32,
                      "quantity": 2,
                      "reason": "QC fail scratched cookware"
                    }
                  ]
                }
                """;
    }

    private String pickQcResultJson() {
        return """
                {
                  "idempotencyKey": "qc-100",
                  "results": [
                    {
                      "doItemId": 200,
                      "allocationId": 900,
                      "batchId": 71,
                      "locationId": 801,
                      "zoneId": 31,
                      "pickedQty": 10,
                      "qcPassQty": 8,
                      "qcFailQty": 2,
                      "qcFailReason": "Surface scratch",
                      "stagingLocationId": 880,
                      "quarantineLocationId": 990,
                      "notes": "Checked at source bin"
                    }
                  ]
                }
                """;
    }

    private String warehouseRejectJson() {
        return """
                {
                  "reason": "Seal issue found before loading",
                  "returnToBinRecords": [
                    {
                      "doItemId": 200,
                      "allocationId": 900,
                      "batchId": 71,
                      "returnedQty": 8,
                      "sourceLocationId": 880,
                      "originalLocationId": 801,
                      "originalZoneId": 31,
                      "reason": "Return staged goods after reject"
                    }
                  ]
                }
                """;
    }

    private String returnedCountQcJson() {
        return """
                {
                  "notes": "Returned goods counted",
                  "items": [
                    {
                      "doItemId": 200,
                      "productId": 30,
                      "batchId": 71,
                      "actualQty": 8,
                      "qualityPassQty": 8,
                      "qualityFailQty": 0
                    }
                  ]
                }
                """;
    }

    private String returnedPutawayPlanJson() {
        return """
                {
                  "notes": "Plan returned goods putaway",
                  "items": [
                    {
                      "doItemId": 200,
                      "batchId": 71,
                      "destinationLocationId": 801,
                      "plannedQty": 8
                    }
                  ]
                }
                """;
    }

    private DeliveryOrderResponse response(DeliveryOrderStatus status) {
        return DeliveryOrderResponse.builder()
                .id(100L)
                .doNumber("DO-20260618-0001")
                .dealerId(10L)
                .warehouseId(20L)
                .type(DeliveryOrderType.SALE)
                .documentDate(LocalDate.of(2026, 6, 18))
                .expectedDeliveryDate(LocalDate.of(2026, 6, 20))
                .status(status)
                .items(List.of(DeliveryOrderItemResponse.builder()
                        .id(200L)
                        .productId(30L)
                        .requestedQty(java.math.BigDecimal.TEN)
                        .plannedQty(java.math.BigDecimal.TEN)
                        .reservedQty(java.math.BigDecimal.TEN)
                        .allocations(List.of(DeliveryOrderAllocationResponse.builder()
                                .allocationId(900L)
                                .inventoryId(501L)
                                .batchId(71L)
                                .locationId(801L)
                                .zoneId(31L)
                                .plannedQty(java.math.BigDecimal.TEN)
                                .pickedQty(java.math.BigDecimal.ZERO)
                                .replacement(false)
                                .build()))
                        .build()))
                .build();
    }

    private ReturnedGoodsFlowResponse returnedFlowResponse(ReturnedDeliveryFlowStatus flowStatus,
                                                           DeliveryOrderStatus deliveryOrderStatus) {
        return ReturnedGoodsFlowResponse.builder()
                .doId(100L)
                .doNumber("DO-20260618-0001")
                .deliveryOrderStatus(deliveryOrderStatus)
                .flowStatus(flowStatus)
                .items(List.of(ReturnedGoodsFlowItemResponse.builder()
                        .doItemId(200L)
                        .productId(30L)
                        .batchId(71L)
                        .expectedQty(java.math.BigDecimal.valueOf(8))
                        .actualQty(java.math.BigDecimal.valueOf(8))
                        .qualityPassQty(java.math.BigDecimal.valueOf(8))
                        .qualityFailQty(java.math.BigDecimal.ZERO)
                        .destinationLocationId(801L)
                        .plannedQty(java.math.BigDecimal.valueOf(8))
                        .putawayCompletedQty(flowStatus == ReturnedDeliveryFlowStatus.PUTAWAY_COMPLETED
                                ? java.math.BigDecimal.valueOf(8)
                                : null)
                        .build()))
                .build();
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(role.name());
        return user;
    }
}

