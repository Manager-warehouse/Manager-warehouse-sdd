package com.wms.controller;

import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderUpdateRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.User;
import com.wms.service.CurrentUserService;
import com.wms.service.DeliveryOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER')")
    @Operation(summary = "Create delivery order")
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ACCOUNTANT','ACCOUNTANT_MANAGER','PLANNER')")
    @Operation(summary = "Cancel delivery order")
    public void cancelDeliveryOrder(@PathVariable Long id) {
        deliveryOrderService.cancelDeliveryOrder(id, currentUser());
    }

    private User currentUser() {
        return currentUserService.getRequiredCurrentUser();
    }
}
