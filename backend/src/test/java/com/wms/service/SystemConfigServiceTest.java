package com.wms.service;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.SystemConfig;
import com.wms.entity.User;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.SystemConfigMapper;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.SystemConfigRepository;
import com.wms.repository.UserRepository;
import com.wms.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    private User adminUser;
    private SystemConfig mockConfig;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);

        mockConfig = new SystemConfig();
        mockConfig.setId(10L);
        mockConfig.setConfigKey("DEFAULT_CREDIT_LIMIT");
        mockConfig.setConfigValue("10000000");
    }

    @Test
    void testUpdateConfig_Success() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("20000000");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("DEFAULT_CREDIT_LIMIT")
                .configValue("20000000")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L);

        assertNotNull(response);
        assertEquals("20000000", response.getConfigValue());

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
        verify(systemConfigRepository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    void testUpdateConfig_InvalidCreditLimit_Negative() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-5000");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L));

        assertTrue(exception.getMessage().contains("must be > 0"));
    }

    @Test
    void testUpdateConfig_InvalidCreditLimit_Zero() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L));

        assertTrue(exception.getMessage().contains("must be > 0"));
    }

    @Test
    void testUpdateConfig_Success_DefaultPaymentTermDays() {
        mockConfig.setConfigKey("DEFAULT_PAYMENT_TERM_DAYS");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("30");

        when(systemConfigRepository.findByConfigKey("DEFAULT_PAYMENT_TERM_DAYS")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("DEFAULT_PAYMENT_TERM_DAYS")
                .configValue("30")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("DEFAULT_PAYMENT_TERM_DAYS", request, 1L);

        assertNotNull(response);
        assertEquals("30", response.getConfigValue());
    }

    @Test
    void testUpdateConfig_InvalidPaymentTermDays_Zero() {
        mockConfig.setConfigKey("DEFAULT_PAYMENT_TERM_DAYS");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");

        when(systemConfigRepository.findByConfigKey("DEFAULT_PAYMENT_TERM_DAYS")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("DEFAULT_PAYMENT_TERM_DAYS", request, 1L));

        assertTrue(exception.getMessage().contains("must be > 0"));
    }

    @Test
    void testUpdateConfig_Success_CreditHoldOverdueDays() {
        mockConfig.setConfigKey("CREDIT_HOLD_OVERDUE_DAYS");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("15");

        when(systemConfigRepository.findByConfigKey("CREDIT_HOLD_OVERDUE_DAYS")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("CREDIT_HOLD_OVERDUE_DAYS")
                .configValue("15")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("CREDIT_HOLD_OVERDUE_DAYS", request, 1L);

        assertNotNull(response);
        assertEquals("15", response.getConfigValue());
    }

    @Test
    void testUpdateConfig_InvalidCreditHoldOverdueDays_Negative() {
        mockConfig.setConfigKey("CREDIT_HOLD_OVERDUE_DAYS");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-5");

        when(systemConfigRepository.findByConfigKey("CREDIT_HOLD_OVERDUE_DAYS")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("CREDIT_HOLD_OVERDUE_DAYS", request, 1L));

        assertTrue(exception.getMessage().contains("must be > 0"));
    }

    @Test
    void testUpdateConfig_Success_CreditUnlockBufferPct() {
        mockConfig.setConfigKey("CREDIT_UNLOCK_BUFFER_PCT");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0.95");

        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("CREDIT_UNLOCK_BUFFER_PCT")
                .configValue("0.95")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L);

        assertNotNull(response);
        assertEquals("0.95", response.getConfigValue());
    }

    @Test
    void testUpdateConfig_InvalidUnlockBufferPct_Zero() {
        mockConfig.setConfigKey("CREDIT_UNLOCK_BUFFER_PCT");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0.0");

        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L));

        assertTrue(exception.getMessage().contains("must be between (0, 1]"));
    }

    @Test
    void testUpdateConfig_InvalidUnlockBufferPct_GreaterThan1() {
        mockConfig.setConfigKey("CREDIT_UNLOCK_BUFFER_PCT");
        
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("1.5");

        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("CREDIT_UNLOCK_BUFFER_PCT", request, 1L));

        assertTrue(exception.getMessage().contains("must be between (0, 1]"));
    }

    @Test
    void testUpdateConfig_Success_MonthlyClosingDay() {
        mockConfig.setConfigKey("MONTHLY_CLOSING_DAY");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("25");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("MONTHLY_CLOSING_DAY")
                .configValue("25")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L);

        assertNotNull(response);
        assertEquals("25", response.getConfigValue());
    }

    @Test
    void testUpdateConfig_InvalidMonthlyClosingDay_Zero() {
        mockConfig.setConfigKey("MONTHLY_CLOSING_DAY");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("0");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L));

        assertTrue(exception.getMessage().contains("must be between 1 and 31"));
    }

    @Test
    void testUpdateConfig_InvalidMonthlyClosingDay_GreaterThan31() {
        mockConfig.setConfigKey("MONTHLY_CLOSING_DAY");
        
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("35");

        when(systemConfigRepository.findByConfigKey("MONTHLY_CLOSING_DAY")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("MONTHLY_CLOSING_DAY", request, 1L));

        assertTrue(exception.getMessage().contains("must be between 1 and 31"));
    }

    @Test
    void testUpdateConfig_Success_MinInventoryWarningThreshold() {
        mockConfig.setConfigKey("MIN_INVENTORY_WARNING_THRESHOLD");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("5");

        when(systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")).thenReturn(Optional.of(mockConfig));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(mockConfig);
        
        SystemConfigResponse responseMock = SystemConfigResponse.builder()
                .configKey("MIN_INVENTORY_WARNING_THRESHOLD")
                .configValue("5")
                .build();
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(responseMock);

        SystemConfigResponse response = systemConfigService.updateConfig("MIN_INVENTORY_WARNING_THRESHOLD", request, 1L);

        assertNotNull(response);
        assertEquals("5", response.getConfigValue());
    }

    @Test
    void testUpdateConfig_InvalidMinInventoryWarningThreshold_Negative() {
        mockConfig.setConfigKey("MIN_INVENTORY_WARNING_THRESHOLD");
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("-1");

        when(systemConfigRepository.findByConfigKey("MIN_INVENTORY_WARNING_THRESHOLD")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("MIN_INVENTORY_WARNING_THRESHOLD", request, 1L));

        assertTrue(exception.getMessage().contains("must be >= 0"));
    }

    @Test
    void testUpdateConfig_EmptyValue() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("  ");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L));

        assertTrue(exception.getMessage().contains("Value cannot be empty"));
    }

    @Test
    void testUpdateConfig_InvalidNumberFormat() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("not_a_number");

        when(systemConfigRepository.findByConfigKey("DEFAULT_CREDIT_LIMIT")).thenReturn(Optional.of(mockConfig));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                systemConfigService.updateConfig("DEFAULT_CREDIT_LIMIT", request, 1L));

        assertTrue(exception.getMessage().contains("Invalid number format for key"));
    }

    @Test
    void testUpdateConfig_NotFoundConfigKey() {
        SystemConfigUpdateRequest request = new SystemConfigUpdateRequest();
        request.setConfigValue("10");

        when(systemConfigRepository.findByConfigKey("UNKNOWN_KEY")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                systemConfigService.updateConfig("UNKNOWN_KEY", request, 1L));
    }
}
