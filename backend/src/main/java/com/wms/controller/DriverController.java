package com.wms.controller;

import com.wms.dto.request.DriverRequest;
import com.wms.dto.request.DriverStatusRequest;
import com.wms.dto.response.DriverResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatcher/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER', 'PLANNER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<List<DriverResponse>> getAllDrivers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(driverService.getAllDrivers(status, isActive));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER', 'PLANNER', 'WAREHOUSE_MANAGER')")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponse> createDriver(
            @Valid @RequestBody DriverRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        DriverResponse response = driverService.createDriver(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        DriverResponse response = driverService.updateDriver(id, request, actorId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<DriverResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusRequest request,
            Principal principal) {
        Long actorId = getActorId(principal);
        DriverResponse response = driverService.updateStatus(id, request.getStatus(), actorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN', 'DISPATCHER')")
    public ResponseEntity<Void> deactivateDriver(
            @PathVariable Long id,
            Principal principal) {
        Long actorId = getActorId(principal);
        driverService.deactivateDriver(id, actorId);
        return ResponseEntity.noContent().build();
    }

    private Long getActorId(Principal principal) {
        String email = principal != null ? principal.getName() : "admin@wms.com";
        User actor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return actor.getId();
    }
}
