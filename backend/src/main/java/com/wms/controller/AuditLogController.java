package com.wms.controller;

import com.wms.dto.response.AuditLogDetailResponse;
import com.wms.dto.response.AuditLogPageResponse;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.repository.UserRepository;
import com.wms.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@Tag(name = "Audit Log", description = "Read-only system audit log endpoints")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public AuditLogController(AuditLogService auditLogService,
            UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs", description = "System Admin only. Returns newest logs first with page-based pagination.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs")
    @ApiResponse(responseCode = "400", description = "Invalid filter or page request")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN_AUDIT_ACCESS")
    public AuditLogPageResponse getAuditLogs(
            @Parameter(description = "1-based page number") @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size, max 30") @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @Parameter(description = "Start date/datetime inclusive") @RequestParam(value = "from", required = false) String from,
            @Parameter(description = "End date/datetime inclusive") @RequestParam(value = "to", required = false) String to,
            @Parameter(description = "Filter by warehouse ID") @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            Authentication authentication) {

        ensureAdmin(authentication);
        return auditLogService.getAuditLogs(
                page, pageSize, from, to, warehouseId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit log detail", description = "System Admin only. Returns changed field before/after values.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved audit log detail")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN_AUDIT_ACCESS")
    @ApiResponse(responseCode = "404", description = "AUDIT_LOG_NOT_FOUND")
    public AuditLogDetailResponse getAuditLogById(
            @PathVariable Long id,
            Authentication authentication) {
        ensureAdmin(authentication);
        return auditLogService.getAuditLogById(id);
    }

    private void ensureAdmin(Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "FORBIDDEN_AUDIT_ACCESS");
        }
    }

    private User resolveCurrentUser(Authentication authentication) {
        Authentication auth = authentication != null
                ? authentication
                : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        String username = auth.getName();
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByCode(username))
                .orElse(null);
    }
}
