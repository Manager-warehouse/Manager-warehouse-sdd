package com.wms.controller;

import com.wms.dto.request.WarehouseRequest;
import com.wms.dto.response.WarehouseResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'PLANNER', 'DISPATCHER')")
    public ResponseEntity<List<WarehouseResponse>> getAllWarehouses(
            @RequestParam(required = false) Boolean isActive,
            Principal principal) {
        Long actorId = getActorId(principal);
        return ResponseEntity.ok(warehouseService.getAllWarehouses(isActive, actorId));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'PLANNER', 'DISPATCHER')")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody WarehouseRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseResponse response = warehouseService.createWarehouse(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseResponse response = warehouseService.updateWarehouse(id, request, actorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<Void> deactivateWarehouse(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        warehouseService.deactivateWarehouse(id, actorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<WarehouseResponse> reactivateWarehouse(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        WarehouseResponse response = warehouseService.reactivateWarehouse(id, actorId);
        return ResponseEntity.ok(response);
    }

    private Long getActorId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@wms.com";
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return actor.getId();
    }
}
