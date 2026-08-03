package com.wms.controller.order_fulfillment;


import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderPickQcResultRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanAdjustmentRequest;
import com.wms.dto.request.DeliveryOrderQualityApprovalRequest;
import com.wms.dto.request.DeliveryOrderReplacementPlanRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseRejectRequest;
import com.wms.dto.request.ReturnedGoodsApprovalRequest;
import com.wms.dto.request.ReturnedGoodsCountQcRequest;
import com.wms.dto.request.ReturnedGoodsPutawayCompleteRequest;
import com.wms.dto.request.ReturnedGoodsPutawayPlanRequest;
import com.wms.dto.request.ReturnedGoodsReceiveRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.dto.response.PickingCandidateResponse;
import com.wms.dto.response.ReturnedGoodsFlowResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.service.order_fulfillment.DeliveryOrderService;
import com.wms.service.user_context.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery-orders")
@Tag(name = "Delivery Orders", description = "Delivery Order CRUD")
public class DeliveryOrderController {

    private final DeliveryOrderService deliveryOrderService;
    private final CurrentUserService currentUserService;

    public DeliveryOrderController(DeliveryOrderService deliveryOrderService,
                                   CurrentUserService currentUserService) {
        this.deliveryOrderService = deliveryOrderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER','STOREKEEPER','WAREHOUSE_STAFF','WAREHOUSE_MANAGER','DISPATCHER','ADMIN','CEO')")
    @Operation(summary = "List delivery orders")
    public List<DeliveryOrderResponse> getAllDeliveryOrders() {
        return deliveryOrderService.getAllDeliveryOrders(currentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER','STOREKEEPER','WAREHOUSE_STAFF','WAREHOUSE_MANAGER','DISPATCHER','ADMIN','CEO')")
    @Operation(summary = "Get delivery order detail")
    public DeliveryOrderResponse getDeliveryOrderById(@PathVariable Long id) {
        return deliveryOrderService.getDeliveryOrderById(id, currentUser());
    }

    @GetMapping("/{id}/picking-candidates")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(
            summary = "Get date-ranked picking candidates for each delivery order item",
            description = "Returns available inventory rows ordered by receivedDate ASC for display only; "
                    + "Storekeeper may select any valid row regardless of its position in the list. "
                    + "Candidates are grouped by DO item ID. "
                    + "Only available when the delivery order status is NEW, WAITING_PICKING, or QC_PENDING_APPROVAL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Picking candidates returned"),
            @ApiResponse(responseCode = "403", description = "Storekeeper not assigned to this warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order not found", content = @Content)
    })
    public Map<Long, List<PickingCandidateResponse>> getPickingCandidates(@PathVariable Long id) {
        return deliveryOrderService.getPickingCandidates(id, currentUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create delivery order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Delivery order created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Planner is not assigned to the selected warehouse", content = @Content),
            @ApiResponse(responseCode = "422", description = "Credit hold, insufficient stock, PRODUCT_WEIGHT_MISSING, or DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY", content = @Content)
    })
    public DeliveryOrderResponse createDeliveryOrder(@Valid @RequestBody DeliveryOrderCreateRequest request) {
        return deliveryOrderService.createDeliveryOrder(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Update delivery order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery order updated"),
            @ApiResponse(responseCode = "400", description = "Invalid update payload or delivery date", content = @Content),
            @ApiResponse(responseCode = "403", description = "Planner is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order not found", content = @Content),
            @ApiResponse(responseCode = "422", description = "Update forbidden, PRODUCT_WEIGHT_MISSING, or DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY", content = @Content)
    })
    public DeliveryOrderResponse updateDeliveryOrder(@PathVariable Long id,
                                                     @Valid @RequestBody DeliveryOrderUpdateRequest request) {
        return deliveryOrderService.updateDeliveryOrder(id, request, currentUser());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLANNER', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Cancel delivery order before warehouse approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery order cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid cancel reason", content = @Content),
            @ApiResponse(responseCode = "403", description = "Actor is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order not found", content = @Content),
            @ApiResponse(responseCode = "422", description = "Delivery order cannot be cancelled in its current status", content = @Content)
    })
    public DeliveryOrderResponse cancelDeliveryOrder(@PathVariable Long id,
                                                     @Valid @RequestBody DeliveryOrderCancelRequest request) {
        return deliveryOrderService.cancelDeliveryOrder(id, request, currentUser());
    }

    @PutMapping("/{id}/picking-plan")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(
            summary = "Save or revise delivery order picking plan",
            description = "Stores the full picking allocation set for a delivery order. "
                    + "When revising a WAITING_PICKING plan, returnToBinRecords are required only for picked allocations "
                    + "that are reduced or removed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Picking plan saved"),
            @ApiResponse(responseCode = "400", description = "Invalid picking-plan payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Storekeeper is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order or inventory row not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inventory, reservation, allocation, or Delivery Order concurrent modification conflict", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Picking-plan business rule violation such as PICKING_PLAN_QTY_MISMATCH, "
                            + "PICKED_GOODS_RETURN_REQUIRED, INVENTORY_ROW_INVALID, or DELIVERY_ORDER_STATUS_INVALID",
                    content = @Content)
    })
    public DeliveryOrderResponse saveDeliveryOrderPickingPlan(@PathVariable Long id,
                                                              @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                      description = "Full revised allocation list. "
                                                                              + "returnToBinRecords are required only for picked allocations being removed or reduced.",
                                                                      required = true)
                                                              @Valid @RequestBody DeliveryOrderPickingPlanRequest request) {
        return deliveryOrderService.saveDeliveryOrderPickingPlan(id, request, currentUser());
    }

    @PutMapping("/{id}/pick-qc-result")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    @Operation(
            summary = "Save one complete outbound pick and QC result set",
            description = "Stores one full active pick/QC cycle for the delivery order. "
                    + "Inventory remains reserved at source until Storekeeper quality approval. "
                    + "Duplicate allocation submission is blocked unless the same idempotency key "
                    + "and exact same payload are replayed after a previous success."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pick/QC result saved"),
            @ApiResponse(responseCode = "400", description = "Invalid pick/QC payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Warehouse staff is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order, allocation, or location not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict", content = @Content),
            @ApiResponse(responseCode = "422", description = "Pick/QC validation or destination location state failed", content = @Content)
    })
    public DeliveryOrderResponse saveDeliveryOrderPickQcResult(@PathVariable Long id,
                                                               @Valid @RequestBody DeliveryOrderPickQcResultRequest request) {
        return deliveryOrderService.saveDeliveryOrderPickQcResult(id, request, currentUser());
    }

    @PutMapping("/{id}/picking-plan-adjustment-request")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    @Operation(summary = "Request Storekeeper to revise an imbalanced picking plan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adjustment request recorded"),
            @ApiResponse(responseCode = "400", description = "Reason is missing", content = @Content),
            @ApiResponse(responseCode = "403", description = "Warehouse scope or role is invalid", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Order is not waiting for picking or its picking plan is already balanced",
                    content = @Content)
    })
    public DeliveryOrderResponse requestPickingPlanAdjustment(@PathVariable Long id,
            @Valid @RequestBody DeliveryOrderPickingPlanAdjustmentRequest request) {
        return deliveryOrderService.requestPickingPlanAdjustment(id, request, currentUser());
    }

    @PutMapping("/{id}/replacement-plan")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(
            summary = "Save delivery order replacement plan after QC fail",
            description = "Stores replacement allocations for unresolved QC-failed quantity while the delivery order "
                    + "is in QC_PENDING_APPROVAL and moves the order back to WAITING_PICKING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Replacement plan saved"),
            @ApiResponse(responseCode = "400", description = "Invalid replacement-plan payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Storekeeper is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "404", description = "Delivery order or inventory row not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inventory version conflict", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Replacement-plan business rule violation such as QC_REPLACEMENT_REQUIRED, "
                            + "INVENTORY_ROW_INVALID, or DELIVERY_ORDER_STATUS_INVALID",
                    content = @Content)
    })
    public DeliveryOrderResponse saveDeliveryOrderReplacementPlan(@PathVariable Long id,
                                                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                          description = "Replacement rows for unresolved QC-failed quantity.",
                                                                          required = true)
                                                                  @Valid @RequestBody DeliveryOrderReplacementPlanRequest request) {
        return deliveryOrderService.saveDeliveryOrderReplacementPlan(id, request, currentUser());
    }

    @PutMapping("/{id}/quality-approval")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Accept outbound quality or reject it for Warehouse Staff recount")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Quality accepted and moved to QC_COMPLETED, or rejected and returned to WAITING_PICKING"),
            @ApiResponse(responseCode = "400", description = "Invalid quality-approval payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Storekeeper is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inventory version or row conflict while applying approved QC movement", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Validation, destination state, or capacity failed, such as QC_REPLACEMENT_REQUIRED, "
                            + "OUTBOUND_QC_REJECTION_REASON_REQUIRED, or DELIVERY_ORDER_STATUS_INVALID",
                    content = @Content)
    })
    public DeliveryOrderResponse approveDeliveryOrderQuality(@PathVariable Long id,
                                                             @Valid @RequestBody DeliveryOrderQualityApprovalRequest request) {
        return deliveryOrderService.approveDeliveryOrderQuality(id, request, currentUser());
    }

    @PutMapping("/{id}/warehouse-approval")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Approve outbound release after QC completion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse approval saved and delivery order moved to WAREHOUSE_APPROVED"),
            @ApiResponse(responseCode = "400", description = "Invalid warehouse-approval payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Warehouse Manager is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "422", description = "Warehouse approval validation failed, such as DELIVERY_ORDER_STATUS_INVALID", content = @Content)
    })
    public DeliveryOrderResponse approveDeliveryOrderWarehouseRelease(@PathVariable Long id,
                                                                     @Valid @RequestBody DeliveryOrderWarehouseApprovalRequest request) {
        return deliveryOrderService.approveDeliveryOrderWarehouseRelease(id, request, currentUser());
    }

    @PutMapping("/{id}/warehouse-reject")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Reject outbound release and return staged goods to source bins")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Warehouse rejection saved and delivery order moved to REJECTED"),
            @ApiResponse(responseCode = "400", description = "Invalid warehouse-reject payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Warehouse Manager is not assigned to the delivery order warehouse", content = @Content),
            @ApiResponse(responseCode = "409", description = "Inventory version conflict while returning staged goods", content = @Content),
            @ApiResponse(responseCode = "422", description = "Warehouse reject validation failed, such as PICKED_GOODS_RETURN_REQUIRED, INVENTORY_ROW_INVALID, or DELIVERY_ORDER_STATUS_INVALID", content = @Content)
    })
    public DeliveryOrderResponse rejectDeliveryOrderWarehouseRelease(@PathVariable Long id,
                                                                    @Valid @RequestBody DeliveryOrderWarehouseRejectRequest request) {
        return deliveryOrderService.rejectDeliveryOrderWarehouseRelease(id, request, currentUser());
    }

    @PutMapping("/{id}/returned-goods/receive")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Confirm returned goods arrived back at the warehouse")
    public ReturnedGoodsFlowResponse confirmReturnedGoodsReceived(@PathVariable Long id,
                                                                  @Valid @RequestBody(required = false) ReturnedGoodsReceiveRequest request) {
        ReturnedGoodsReceiveRequest safeRequest = request == null ? new ReturnedGoodsReceiveRequest() : request;
        return deliveryOrderService.confirmReturnedGoodsReceived(id, safeRequest, currentUser());
    }

    @PutMapping("/{id}/returned-goods/count-qc")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    @Operation(summary = "Submit or resubmit returned goods count and QC for a failed delivery")
    public ReturnedGoodsFlowResponse submitReturnedGoodsCountQc(@PathVariable Long id,
                                                                @Valid @RequestBody ReturnedGoodsCountQcRequest request) {
        return deliveryOrderService.submitReturnedGoodsCountQc(id, request, currentUser());
    }

    @GetMapping("/{id}/returned-goods")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF','STOREKEEPER','WAREHOUSE_MANAGER')")
    @Operation(summary = "Get returned goods flow state")
    public ReturnedGoodsFlowResponse getReturnedGoodsFlow(@PathVariable Long id) {
        return deliveryOrderService.getReturnedGoodsFlow(id, currentUser());
    }

    @PutMapping("/{id}/returned-goods/approval")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Accept or reject returned goods quantity and quality")
    public ReturnedGoodsFlowResponse approveReturnedGoods(@PathVariable Long id,
                                                          @Valid @RequestBody ReturnedGoodsApprovalRequest request) {
        return deliveryOrderService.approveReturnedGoods(id, request, currentUser());
    }

    @PutMapping("/{id}/returned-goods/putaway-plan")
    @PreAuthorize("hasRole('STOREKEEPER')")
    @Operation(summary = "Plan putaway for approved returned goods")
    public ReturnedGoodsFlowResponse planReturnedGoodsPutaway(@PathVariable Long id,
                                                              @Valid @RequestBody ReturnedGoodsPutawayPlanRequest request) {
        return deliveryOrderService.planReturnedGoodsPutaway(id, request, currentUser());
    }

    @PutMapping("/{id}/returned-goods/putaway-complete")
    @PreAuthorize("hasRole('WAREHOUSE_STAFF')")
    @Operation(summary = "Confirm returned goods putaway and close failed delivery")
    public ReturnedGoodsFlowResponse completeReturnedGoodsPutaway(@PathVariable Long id,
                                                                  @Valid @RequestBody ReturnedGoodsPutawayCompleteRequest request) {
        return deliveryOrderService.completeReturnedGoodsPutaway(id, request, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
