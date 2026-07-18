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
import com.wms.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
        String newValue = request.getConfigValue();
        SystemConfigKey configEnum;
        try {
            configEnum = SystemConfigKey.valueOf(configKey);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Configuration not found for key: " + configKey);
        }
        validateConfigValue(configEnum, newValue);

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + adminUserId));
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseGet(() -> SystemConfig.builder()
                        .configKey(configKey)
                        .description(resolveDefaultDescription(configKey))
                        .build());

        String oldValue = config.getConfigValue();
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
                .description(AuditLogUtil.generateDescription(
                        AuditAction.UPDATE,
                        "SystemConfig",
                        savedConfig.getConfigKey()))
                .oldValue(AuditLogUtil.toJson(Map.of("config_value", oldValue != null ? oldValue : "")))
                .newValue(AuditLogUtil.toJson(Map.of("config_value", newValue)))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(auditLog);

        return systemConfigMapper.toResponse(savedConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public int getIntValue(String configKey, int defaultValue) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDecimalValue(String configKey, BigDecimal defaultValue) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private String resolveDefaultDescription(String configKey) {
        try {
            SystemConfigKey key = SystemConfigKey.valueOf(configKey);
            return switch (key) {
                case DEFAULT_CREDIT_LIMIT -> "Hạn mức công nợ mặc định (VNĐ)";
                case DEFAULT_PAYMENT_TERM_DAYS -> "Kỳ hạn thanh toán mặc định (ngày)";
                case CREDIT_HOLD_OVERDUE_DAYS -> "Số ngày quá hạn trước khi khóa tín dụng";
                case CREDIT_UNLOCK_BUFFER_PCT -> "Ngưỡng mở khóa tín dụng";
                case MONTHLY_CLOSING_DAY -> "Ngày khóa sổ kỳ kế toán hàng tháng";
                case MIN_INVENTORY_WARNING_THRESHOLD -> "Ngưỡng cảnh báo tồn kho tối thiểu mặc định";
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void validateConfigValue(SystemConfigKey key, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value cannot be empty");
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
            throw new IllegalArgumentException("Invalid number format for key: " + key.name());
        }
    }
}
