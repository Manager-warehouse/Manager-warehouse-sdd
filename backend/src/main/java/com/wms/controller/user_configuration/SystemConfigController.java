package com.wms.controller.user_configuration;


import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.access_control.User;
import com.wms.service.user_configuration.SystemConfigService;
import com.wms.service.user_context.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cấu hình hệ thống (Spec 001) — chỉ dành cho ADMIN.
 * Cho phép xem và cập nhật các tham số hệ thống: hạn mức công nợ, kỳ thanh toán, ngày khóa sổ, v.v.
 */
@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfigResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody SystemConfigUpdateRequest request) {
        User adminUser = currentUserService.getRequiredCurrentUser();
        SystemConfigResponse response = systemConfigService.updateConfig(configKey, request, adminUser.getId());
        return ResponseEntity.ok(response);
    }
}
