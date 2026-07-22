package com.wms.controller.fleet_management;


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
import com.wms.dto.request.VehicleRequest;
import com.wms.dto.request.VehicleStatusRequest;
import com.wms.dto.response.VehicleResponse;
import com.wms.entity.access_control.User;
import com.wms.repository.UserRepository;
import com.wms.service.fleet_management.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatcher/vehicles")//sfatydfa
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER', 'PLANNER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(vehicleService.getAllVehicles(status, isActive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER', 'PLANNER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        VehicleResponse response = vehicleService.createVehicle(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        VehicleResponse response = vehicleService.updateVehicle(id, request, actorId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleStatusRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        VehicleResponse response = vehicleService.updateStatus(id, request.getStatus(), actorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<Void> deactivateVehicle(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        vehicleService.deactivateVehicle(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<VehicleResponse> reactivateVehicle(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        VehicleResponse response = vehicleService.reactivateVehicle(id, actorId);
        return ResponseEntity.ok(response);
    }

    private Long getActorId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@wms.com";
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return actor.getId();
    }
}
