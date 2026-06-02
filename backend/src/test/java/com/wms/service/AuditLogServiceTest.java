package com.wms.service;

import com.wms.entity.AuditLog;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    private User testActor;

    @BeforeEach
    void setUp() {
        testActor = new User();
        testActor.setId(1L);
        testActor.setFullName("Nguyen Van A");
        testActor.setRole(UserRole.STOREKEEPER);
    }

    @Test
    void testLog_createsEntryWithCorrectFields() {
        Map<String, Object> oldVal = Map.of("status", "DRAFT");
        Map<String, Object> newVal = Map.of("status", "APPROVED");

        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditLogService.log(
                testActor,
                AuditAction.APPROVE,
                "RECEIPT",
                101L,
                "PN-2026-001",
                2L,
                oldVal,
                newVal
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(testActor, saved.getActor());
        assertEquals("STOREKEEPER", saved.getActorRole());
        assertEquals(AuditAction.APPROVE, saved.getAction());
        assertEquals("RECEIPT", saved.getEntityType());
        assertEquals(101L, saved.getEntityId());
        assertEquals("APPROVE RECEIPT PN-2026-001", saved.getDescription());
        assertNotNull(saved.getWarehouse());
        assertEquals(2L, saved.getWarehouse().getId());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertTrue(saved.getOldValue().contains("DRAFT"));
        assertTrue(saved.getNewValue().contains("APPROVED"));
    }

    @Test
    void testLog_filtersSensitiveFields() {
        Map<String, Object> oldVal = Map.of("passwordHash", "secret123", "status", "DRAFT");
        Map<String, Object> newVal = Map.of("passwordHash", "newsecret", "status", "ACTIVE");

        auditLogService.log(
                testActor,
                AuditAction.UPDATE,
                "USER",
                1L,
                "USR001",
                null,
                oldVal,
                newVal
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertNotNull(saved);
        assertFalse(saved.getOldValue().contains("passwordHash"));
        assertFalse(saved.getNewValue().contains("passwordHash"));
        assertTrue(saved.getOldValue().contains("DRAFT"));
        assertTrue(saved.getNewValue().contains("ACTIVE"));
    }

    @Test
    void testLog_resolvesIpFromXForwardedFor() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        auditLogService.log(
                testActor,
                AuditAction.CREATE,
                "BATCH",
                50L,
                "BATCH-001",
                null,
                null,
                Map.of("quantity", 100)
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("192.168.1.1", saved.getIpAddress());
    }

    @Test
    void testLog_handlesNullWarehouseId() {
        auditLogService.log(
                testActor,
                AuditAction.CREATE,
                "PRODUCT",
                200L,
                "PROD-01",
                null,
                null,
                Map.of("name", "Product A")
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertNull(saved.getWarehouse());
    }
}
