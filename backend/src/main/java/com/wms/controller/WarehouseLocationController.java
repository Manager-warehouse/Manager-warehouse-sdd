package com.wms.controller;

import com.wms.dto.request.WarehouseLocationRequest;
import com.wms.dto.response.CapacityResponse;
import com.wms.dto.response.WarehouseLocationResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.WarehouseLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/warehouse-locations")
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService locationService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'PLANNER')")
    public ResponseEntity<List<WarehouseLocationResponse>> getAllLocations(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isQuarantine,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(locationService.getAllLocations(warehouseId, type, isQuarantine, isActive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'PLANNER')")
    public ResponseEntity<WarehouseLocationResponse> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    @GetMapping("/{id}/capacity")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'PLANNER')")
    public ResponseEntity<CapacityResponse> getCapacity(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getCapacity(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<WarehouseLocationResponse> createLocation(
            @Valid @RequestBody WarehouseLocationRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseLocationResponse response = locationService.createLocation(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<WarehouseLocationResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseLocationRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseLocationResponse response = locationService.updateLocation(id, request, actorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<Void> deactivateLocation(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        locationService.deactivateLocation(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<WarehouseLocationResponse> reactivateLocation(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseLocationResponse response = locationService.reactivateLocation(id, actorId);
        return ResponseEntity.ok(response);
    }

    private Long getActorId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@wms.com";
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return actor.getId();
    }
}
