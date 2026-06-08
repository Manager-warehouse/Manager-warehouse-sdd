package com.wms.controller;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.User;
import com.wms.repository.UserRepository;
import com.wms.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        User adminUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        SystemConfigResponse response = systemConfigService.updateConfig(configKey, request, adminUser.getId());
        return ResponseEntity.ok(response);
    }
}
