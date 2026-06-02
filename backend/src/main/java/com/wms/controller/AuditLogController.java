package com.wms.controller;

import com.wms.dto.AuditLogPageResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Log", description = "Endpoints for system audit logging and compliance tracking")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    public AuditLogController(AuditLogService auditLogService,
                              UserRepository userRepository,
                              UserWarehouseAssignmentRepository userWarehouseAssignmentRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CEO', 'WAREHOUSE_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get system audit logs with cursor-based pagination and filtering",
               description = "Requires ADMIN, CEO, WAREHOUSE_MANAGER, or ACCOUNTANT role. " +
                             "WAREHOUSE_MANAGER role can only retrieve logs for their assigned warehouses.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs")
    @ApiResponse(responseCode = "403", description = "Forbidden - User does not have appropriate permissions")
    public AuditLogPageResponse getAuditLogs(
            @Parameter(description = "Cursor for pagination (ID of the last item in the previous page)")
            @RequestParam(value = "cursor", required = false) Long cursor,
            @Parameter(description = "Page size (default 30, max 100)")
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Filter by actor user ID")
            @RequestParam(value = "actorId", required = false) Long actorId,
            @Parameter(description = "Filter by entity type (e.g., RECEIPT, TRANSFER)")
            @RequestParam(value = "entityType", required = false) String entityType,
            @Parameter(description = "Filter by action (e.g., CREATE, APPROVE)")
            @RequestParam(value = "action", required = false) String action,
            @Parameter(description = "Filter by warehouse ID")
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @Parameter(description = "Filter start date (inclusive, yyyy-MM-dd)")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter end date (inclusive, yyyy-MM-dd)")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        User currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated or not found in system");
        }

        if (currentUser.getRole() != com.wms.enums.UserRole.ADMIN &&
            currentUser.getRole() != com.wms.enums.UserRole.CEO &&
            currentUser.getRole() != com.wms.enums.UserRole.WAREHOUSE_MANAGER &&
            currentUser.getRole() != com.wms.enums.UserRole.ACCOUNTANT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        List<Long> assignedWarehouseIds = null;
        if (currentUser.getRole() == com.wms.enums.UserRole.WAREHOUSE_MANAGER) {
            assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(currentUser.getId());
            if (assignedWarehouseIds.isEmpty()) {
                return new AuditLogPageResponse(List.of(), null, false);
            }
        }

        return auditLogService.getAuditLogs(
                cursor, size, actorId, entityType, action, warehouseId,
                startDate, endDate, currentUser, assignedWarehouseIds
        );
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        
        String username = authentication.getName();
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByCode(username))
                .orElse(null);
    }
}
