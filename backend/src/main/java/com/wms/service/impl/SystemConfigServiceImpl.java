package com.wms.service.impl;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.SystemConfig;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.enums.SystemConfigKey;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.SystemConfigMapper;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.repository.UserRepository;
import com.wms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findAll()
                .stream()
                .map(systemConfigMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SystemConfigResponse updateConfig(String configKey, SystemConfigUpdateRequest request, Long adminUserId) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for key: " + configKey));

        String newValue = request.getConfigValue();
        validateConfigValue(configKey, newValue);

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + adminUserId));

        String oldValue = config.getConfigValue();
        
        // Update config
        config.setConfigValue(newValue);
        config.setUpdatedBy(adminUser);
        config.setUpdatedAt(OffsetDateTime.now());
        
        SystemConfig savedConfig = systemConfigRepository.save(config);

        // Record Audit Log
        AuditLog auditLog = AuditLog.builder()
                .actor(adminUser)
                .actorRole(adminUser.getRole() != null ? adminUser.getRole().name() : "ADMIN")
                .action(AuditAction.UPDATE)
                .entityType("SystemConfig")
                .entityId(savedConfig.getId())
                .oldValue("{\"config_value\": \"" + oldValue + "\"}")
                .newValue("{\"config_value\": \"" + newValue + "\"}")
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return systemConfigMapper.toResponse(savedConfig);
    }

    private void validateConfigValue(String keyStr, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be empty");
        }

        SystemConfigKey key;
        try {
            key = SystemConfigKey.valueOf(keyStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown config key: " + keyStr);
        }

        try {
            switch (key) {
                case DEFAULT_CREDIT_LIMIT:
                    long creditLimit = Long.parseLong(value);
                    if (creditLimit <= 0) throw new IllegalArgumentException("DEFAULT_CREDIT_LIMIT must be > 0");
                    break;
                case DEFAULT_PAYMENT_TERM_DAYS:
                    int paymentTerm = Integer.parseInt(value);
                    if (paymentTerm <= 0) throw new IllegalArgumentException("DEFAULT_PAYMENT_TERM_DAYS must be > 0");
                    break;
                case CREDIT_HOLD_OVERDUE_DAYS:
                    int overdueDays = Integer.parseInt(value);
                    if (overdueDays <= 0) throw new IllegalArgumentException("CREDIT_HOLD_OVERDUE_DAYS must be > 0");
                    break;
                case CREDIT_UNLOCK_BUFFER_PCT:
                    double pct = Double.parseDouble(value);
                    if (pct <= 0 || pct > 1) throw new IllegalArgumentException("CREDIT_UNLOCK_BUFFER_PCT must be between (0, 1]");
                    break;
                case MONTHLY_CLOSING_DAY:
                    int day = Integer.parseInt(value);
                    if (day < 1 || day > 31) throw new IllegalArgumentException("MONTHLY_CLOSING_DAY must be between 1 and 31");
                    break;
                case MIN_INVENTORY_WARNING_THRESHOLD:
                    int threshold = Integer.parseInt(value);
                    if (threshold < 0) throw new IllegalArgumentException("MIN_INVENTORY_WARNING_THRESHOLD must be >= 0");
                    break;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for key: " + keyStr);
        }
    }
}
