package com.wms.controller;

import com.wms.dto.request.DeliveryOrderCancelRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderReplacementPlanRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.DeliveryOrderService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER')")
    @Operation(summary = "List delivery orders")
    public List<DeliveryOrderResponse> getAllDeliveryOrders() {
        return deliveryOrderService.getAllDeliveryOrders();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER')")
    @Operation(summary = "Get delivery order detail")
    public DeliveryOrderResponse getDeliveryOrderById(@PathVariable Long id) {
        return deliveryOrderService.getDeliveryOrderById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLANNER')")
    @Operation(summary = "Create delivery order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Delivery order created"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @ApiResponse(responseCode = "403", description = "Planner is not assigned to the selected warehouse", content = @Content),
            @ApiResponse(responseCode = "422", description = "Credit hold or insufficient stock", content = @Content)
    })
    public DeliveryOrderResponse createDeliveryOrder(@Valid @RequestBody DeliveryOrderCreateRequest request) {
        return deliveryOrderService.createDeliveryOrder(request, currentUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER')")
    @Operation(summary = "Update delivery order")
    public DeliveryOrderResponse updateDeliveryOrder(@PathVariable Long id,
                                                     @Valid @RequestBody DeliveryOrderUpdateRequest request) {
        return deliveryOrderService.updateDeliveryOrder(id, request, currentUser());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('WAREHOUSE_MANAGER')")
    @Operation(summary = "Cancel delivery order before warehouse approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery order cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid cancel reason", content = @Content),
            @ApiResponse(responseCode = "403", description = "Warehouse Manager is not assigned to the delivery order warehouse", content = @Content),
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
            @ApiResponse(responseCode = "409", description = "Inventory or reservation version conflict", content = @Content),
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

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
