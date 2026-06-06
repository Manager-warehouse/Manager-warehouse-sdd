package com.wms.controller;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfigResponse>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfigResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody SystemConfigUpdateRequest request,
            Principal principal) {
        
        // Find admin user id from principal (assuming email is used as principal name)
        String email = principal != null ? principal.getName() : "admin@wms.com";
        User adminUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        SystemConfigResponse response = systemConfigService.updateConfig(configKey, request, adminUser.getId());
        return ResponseEntity.ok(response);
    }
}
