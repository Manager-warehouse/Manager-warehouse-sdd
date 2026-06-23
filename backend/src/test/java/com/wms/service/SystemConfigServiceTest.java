package com.wms.service;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.SystemConfig;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.SystemConfigMapper;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.repository.UserRepository;
import com.wms.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SystemConfigServiceTest {

    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private SystemConfigMapper systemConfigMapper;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    private User adminUser;
    private SystemConfig mockConfig;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("System Admin");
        adminUser.setRole(UserRole.ADMIN);

        mockConfig = new SystemConfig();
        mockConfig.setId(10L);
        mockConfig.setConfigKey("DEFAULT_CREDIT_LIMIT");
        mockConfig.setConfigValue("10000000");
        mockConfig.setUpdatedAt(OffsetDateTime.now());
    }

    // ─── GET ALL CONFIGS ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy toàn bộ config → trả về danh sách đầy đủ")
    void getAllConfigs_returnsAllConfigs() {
        SystemConfig c1 = buildConfig("DEFAULT_CREDIT_LIMIT", "10000000");
        SystemConfig c2 = buildConfig("MONTHLY_CLOSING_DAY", "25");

        SystemConfigResponse r1 = SystemConfigResponse.builder().configKey("DEFAULT_CREDIT_LIMIT").configValue("10000000").build();
        SystemConfigResponse r2 = SystemConfigResponse.builder().configKey("MONTHLY_CLOSING_DAY").configValue("25").build();

        when(systemConfigRepository.findAll()).thenReturn(List.of(c1, c2));
        when(systemConfigMapper.toResponse(c1)).thenReturn(r1);
        when(systemConfigMapper.toResponse(c2)).thenReturn(r2);

        List<SystemConfigResponse> result = systemConfigService.getAllConfigs();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getConfigKey()).isEqualTo("DEFAULT_CREDIT_LIMIT");
        assertThat(result.get(1).getConfigKey()).isEqualTo("MONTHLY_CLOSING_DAY");
    }

    @Test
    @DisplayName("Không có config nào → trả về danh sách rỗng")
    void getAllConfigs_empty_returnsEmptyList() {
        when(systemConfigRepository.findAll()).thenReturn(List.of());

        List<SystemConfigResponse> result = systemConfigService.getAllConfigs();

        assertThat(result).isEmpty();
    }

    // ─── UPDATE CONFIG — DEFAULT_CREDIT_LIMIT ────────────────────────────────

    @Test
    @DisplayName("Cập nhật DEFAULT_CREDIT_LIMIT hợp lệ → lưu thành công + ghi audit log")
    void updateConfig_creditLimit_valid_savesAndLogsAudit() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("800000000");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(mockConfig);
        SystemConfigResponse expected = SystemConfigResponse.builder()
                .configKey("DEFAULT_CREDIT_LIMIT").configValue("800000000").build();
        when(systemConfigMapper.toResponse(any())).thenReturn(expected);

        SystemConfigResponse response = systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L);

        assertThat(response.getConfigValue()).isEqualTo("800000000");

        // Verify audit log ghi đúng old/new value
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog audit = auditCaptor.getValue();
        assertThat(audit.getActor()).isEqualTo(adminUser);
        assertThat(audit.getDescription()).isEqualTo("UPDATE SystemConfig DEFAULT_CREDIT_LIMIT");
        assertThat(audit.getOldValue()).contains("10000000");
        assertThat(audit.getNewValue()).contains("800000000");
    }

    @Test
    @DisplayName("DEFAULT_CREDIT_LIMIT = 0 → 400 validation error")
    void updateConfig_creditLimit_zero_throws() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");
        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be > 0");
    }

    @Test
    @DisplayName("DEFAULT_CREDIT_LIMIT âm → 400 validation error")
    void updateConfig_creditLimit_negative_throws() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-5000");
        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be > 0");
    }

    // ─── UPDATE CONFIG — DEFAULT_PAYMENT_TERM_DAYS ───────────────────────────

    @Test
    @DisplayName("DEFAULT_PAYMENT_TERM_DAYS = 30 → hợp lệ")
    void updateConfig_paymentTermDays_valid() {
        SystemConfig cfg = buildConfig("DEFAULT_PAYMENT_TERM_DAYS", "15");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("30");

        when(systemConfigRepository.findByConfigKey("DEFAULT_PAYMENT_TERM_DAYS")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("DEFAULT_PAYMENT_TERM_DAYS").configValue("30").build());

        SystemConfigResponse response = systemConfigService.updateConfig("DEFAULT_PAYMENT_TERM_DAYS", request, 1L);

        assertThat(response.getConfigValue()).isEqualTo("30");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("DEFAULT_PAYMENT_TERM_DAYS = 0 → 400 validation error")
    void updateConfig_paymentTermDays_zero_throws() {
        SystemConfig cfg = buildConfig("DEFAULT_PAYMENT_TERM_DAYS", "30");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");
        when(systemConfigRepository.findByConfigKey("DEFAULT_PAYMENT_TERM_DAYS")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_PAYMENT_TERM_DAYS", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be > 0");
    }

    // ─── UPDATE CONFIG — CREDIT_HOLD_OVERDUE_DAYS ────────────────────────────

    @Test
    @DisplayName("CREDIT_HOLD_OVERDUE_DAYS = 15 → hợp lệ")
    void updateConfig_creditHoldOverdueDays_valid() {
        SystemConfig cfg = buildConfig("CREDIT_HOLD_OVERDUE_DAYS", "30");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("15");

        when(systemConfigRepository.findByConfigKey("CREDIT_HOLD_OVERDUE_DAYS")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("CREDIT_HOLD_OVERDUE_DAYS").configValue("15").build());

        assertThat(systemConfigService.updateConfig("CREDIT_HOLD_OVERDUE_DAYS", request, 1L).getConfigValue()).isEqualTo("15");
    }

    @Test
    @DisplayName("CREDIT_HOLD_OVERDUE_DAYS âm → 400 validation error")
    void updateConfig_creditHoldOverdueDays_negative_throws() {
        SystemConfig cfg = buildConfig("CREDIT_HOLD_OVERDUE_DAYS", "30");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-5");
        when(systemConfigRepository.findByConfigKey("CREDIT_HOLD_OVERDUE_DAYS")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("CREDIT_HOLD_OVERDUE_DAYS", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be > 0");
    }

    // ─── UPDATE CONFIG — CREDIT_UNLOCK_BUFFER_PCT ────────────────────────────

    @Test
    @DisplayName("CREDIT_UNLOCK_BUFFER_PCT = 0.8 → hợp lệ")
    void updateConfig_creditUnlockBufferPct_valid() {
        SystemConfig cfg = buildConfig("CREDIT_UNLOCK_BUFFER_PCT", "0.7");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0.8");

        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("CREDIT_UNLOCK_BUFFER_PCT").configValue("0.8").build());

        assertThat(systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L).getConfigValue()).isEqualTo("0.8");
    }

    @Test
    @DisplayName("CREDIT_UNLOCK_BUFFER_PCT = 1.0 → hợp lệ (boundary max)")
    void updateConfig_creditUnlockBufferPct_exactlyOne_valid() {
        SystemConfig cfg = buildConfig("CREDIT_UNLOCK_BUFFER_PCT", "0.8");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("1.0");

        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("CREDIT_UNLOCK_BUFFER_PCT").configValue("1.0").build());

        assertThatCode(() -> systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CREDIT_UNLOCK_BUFFER_PCT = 0.0 → 400 (phải > 0)")
    void updateConfig_creditUnlockBufferPct_zero_throws() {
        SystemConfig cfg = buildConfig("CREDIT_UNLOCK_BUFFER_PCT", "0.8");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0.0");
        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between (0, 1]");
    }

    @Test
    @DisplayName("CREDIT_UNLOCK_BUFFER_PCT = 1.5 → 400 (phải <= 1)")
    void updateConfig_creditUnlockBufferPct_greaterThan1_throws() {
        SystemConfig cfg = buildConfig("CREDIT_UNLOCK_BUFFER_PCT", "0.8");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("1.5");
        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between (0, 1]");
    }

    // ─── UPDATE CONFIG — MONTHLY_CLOSING_DAY ─────────────────────────────────

    @Test
    @DisplayName("MONTHLY_CLOSING_DAY = 25 → hợp lệ")
    void updateConfig_monthlyClosingDay_valid() {
        SystemConfig cfg = buildConfig("MONTHLY_CLOSING_DAY", "30");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("25");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("MONTHLY_CLOSING_DAY").configValue("25").build());

        assertThat(systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L).getConfigValue()).isEqualTo("25");
    }

    @Test
    @DisplayName("MONTHLY_CLOSING_DAY = 1 → hợp lệ (boundary min)")
    void updateConfig_monthlyClosingDay_boundaryMin_valid() {
        SystemConfig cfg = buildConfig("MONTHLY_CLOSING_DAY", "25");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("1");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("MONTHLY_CLOSING_DAY").configValue("1").build());

        assertThatCode(() -> systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MONTHLY_CLOSING_DAY = 31 → hợp lệ (boundary max)")
    void updateConfig_monthlyClosingDay_boundaryMax_valid() {
        SystemConfig cfg = buildConfig("MONTHLY_CLOSING_DAY", "25");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("31");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("MONTHLY_CLOSING_DAY").configValue("31").build());

        assertThatCode(() -> systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MONTHLY_CLOSING_DAY = 0 → 400 validation error")
    void updateConfig_monthlyClosingDay_zero_throws() {
        SystemConfig cfg = buildConfig("MONTHLY_CLOSING_DAY", "25");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");
        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between 1 and 31");
    }

    @Test
    @DisplayName("MONTHLY_CLOSING_DAY = 32 → 400 validation error")
    void updateConfig_monthlyClosingDay_greaterThan31_throws() {
        SystemConfig cfg = buildConfig("MONTHLY_CLOSING_DAY", "25");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("32");
        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between 1 and 31");
    }

    // ─── UPDATE CONFIG — MIN_INVENTORY_WARNING_THRESHOLD ─────────────────────

    @Test
    @DisplayName("MIN_INVENTORY_WARNING_THRESHOLD = 5 → hợp lệ")
    void updateConfig_minInventoryThreshold_valid() {
        SystemConfig cfg = buildConfig("MIN_INVENTORY_WARNING_THRESHOLD", "10");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("5");

        when(systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("MIN_INVENTORY_WARNING_THRESHOLD").configValue("5").build());

        assertThat(systemConfigService.updateConfig("MIN_INVENTORY_WARNING_THRESHOLD", request, 1L).getConfigValue()).isEqualTo("5");
    }

    @Test
    @DisplayName("MIN_INVENTORY_WARNING_THRESHOLD = 0 → hợp lệ (>= 0)")
    void updateConfig_minInventoryThreshold_zero_valid() {
        SystemConfig cfg = buildConfig("MIN_INVENTORY_WARNING_THRESHOLD", "5");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");

        when(systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")).thenReturn(Optional.of(cfg));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(cfg);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("MIN_INVENTORY_WARNING_THRESHOLD").configValue("0").build());

        assertThatCode(() -> systemConfigService.updateConfig("MIN_INVENTORY_WARNING_THRESHOLD", request, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MIN_INVENTORY_WARNING_THRESHOLD âm → 400 validation error")
    void updateConfig_minInventoryThreshold_negative_throws() {
        SystemConfig cfg = buildConfig("MIN_INVENTORY_WARNING_THRESHOLD", "5");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-1");
        when(systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("MIN_INVENTORY_WARNING_THRESHOLD", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be >= 0");
    }

    // ─── UPDATE CONFIG — EDGE CASES ───────────────────────────────────────────

    @Test
    @DisplayName("Giá trị trắng (blank) → 400 Value cannot be empty")
    void updateConfig_blankValue_throws() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("  ");
        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value cannot be empty");
    }

    @Test
    @DisplayName("Giá trị không phải số → 400 Invalid number format")
    void updateConfig_notANumber_throws() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("not_a_number");
        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid number format for key");
    }

    @Test
    @DisplayName("configKey không tồn tại trong DB → 404 ResourceNotFoundException")
    void updateConfig_configKeyNotFoundInDb_throws404() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("100");
        when(systemConfigRepository.findByConfigKey("UNKNOWN_KEY")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigService.updateConfig("UNKNOWN_KEY", request, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("configKey không hợp lệ (không có trong enum) → 400 Unknown config key")
    void updateConfig_invalidEnumKey_throwsUnknown() {
        SystemConfig cfg = buildConfig("INVALID_ENUM_KEY", "10");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("10");
        when(systemConfigRepository.findByConfigKey("INVALID_ENUM_KEY")).thenReturn(Optional.of(cfg));

        assertThatThrownBy(() -> systemConfigService.updateConfig("INVALID_ENUM_KEY", request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Configuration not found for key");
    }

    @Test
    @DisplayName("Admin user không tồn tại → 404 ResourceNotFoundException")
    void updateConfig_adminUserNotFound_throws404() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("500000");
        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Audit log ghi đúng entityType=SystemConfig và action=UPDATE")
    void updateConfig_auditLog_hasCorrectEntityTypeAndAction() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("500000");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any())).thenReturn(mockConfig);
        when(systemConfigMapper.toResponse(any())).thenReturn(
                SystemConfigResponse.builder().configKey("DEFAULT_CREDIT_LIMIT").configValue("500000").build());

        systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();

        assertThat(audit.getEntityType()).isEqualTo("SystemConfig");
        assertThat(audit.getAction().name()).isEqualTo("UPDATE");
        assertThat(audit.getActorRole()).isEqualTo("ADMIN");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private SystemConfig buildConfig(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setId(10L);
        c.setConfigKey(key);
        c.setConfigValue(value);
        c.setUpdatedAt(OffsetDateTime.now());
        return c;
    }
}
