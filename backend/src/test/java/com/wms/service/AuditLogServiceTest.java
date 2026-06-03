package com.wms.service;

import com.wms.dto.AuditLogDetailResponse;
import com.wms.dto.AuditLogPageResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.User;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    private User actor;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setFullName("System Admin");
        actor.setRole(UserRole.ADMIN);
    }

    @Test
    void testLog_createsEntryWithCorrectFields() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditLogService.log(
                actor,
                AuditAction.UPDATE,
                "User",
                101L,
                "101",
                2L,
                Map.of("fullName", "Old"),
                Map.of("fullName", "New"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(actor, saved.getActor());
        assertEquals("ADMIN", saved.getActorRole());
        assertEquals(AuditAction.UPDATE, saved.getAction());
        assertEquals("User", saved.getEntityType());
        assertEquals(101L, saved.getEntityId());
        assertEquals("UPDATE User 101", saved.getDescription());
        assertEquals(2L, saved.getWarehouse().getId());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertTrue(saved.getOldValue().contains("Old"));
        assertTrue(saved.getNewValue().contains("New"));
    }

    @Test
    void testLog_omitsSensitiveFields() {
        auditLogService.log(
                actor,
                AuditAction.UPDATE,
                "User",
                1L,
                "1",
                null,
                Map.of("passwordHash", "secret", "email", "a@wms.com"),
                Map.of("passwordHash", "new-secret", "email", "b@wms.com"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertFalse(saved.getOldValue().contains("passwordHash"));
        assertFalse(saved.getNewValue().contains("passwordHash"));
        assertTrue(saved.getOldValue().contains("a@wms.com"));
        assertTrue(saved.getNewValue().contains("b@wms.com"));
        assertNull(saved.getWarehouse());
    }

    @Test
    void testGetAuditLogs_defaultsAndReturnsNewestPage() {
        AuditLog entry = auditLog(10L);
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        AuditLogPageResponse response = auditLogService.getAuditLogs(null, null, null, null, null);

        assertEquals(1, response.getPage());
        assertEquals(30, response.getPageSize());
        assertEquals(1, response.getData().size());
        assertFalse(response.isHasNext());
    }

    @Test
    void testGetAuditLogs_page51WithoutFiltersRejected() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditLogService.getAuditLogs(51, 30, null, null, null));

        assertEquals("QUERY_RANGE_TOO_LARGE", ex.getReason());
    }

    @Test
    void testGetAuditLogById_returnsDetail() {
        when(auditLogRepository.findById(10L)).thenReturn(Optional.of(auditLog(10L)));

        AuditLogDetailResponse response = auditLogService.getAuditLogById(10L);

        assertEquals(10L, response.getId());
        assertEquals("System Admin", response.getActorName());
    }

    @Test
    void testGetAuditLogById_notFound() {
        when(auditLogRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> auditLogService.getAuditLogById(404L));

        assertEquals("AUDIT_LOG_NOT_FOUND", ex.getReason());
    }

    private AuditLog auditLog(Long id) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setActorRole("ADMIN");
        log.setAction(AuditAction.UPDATE);
        log.setEntityType("User");
        log.setEntityId(1L);
        log.setDescription("UPDATE User 1");
        log.setTimestamp(OffsetDateTime.now());
        log.setOldValue("{\"fullName\":\"Old\"}");
        log.setNewValue("{\"fullName\":\"New\"}");
        try {
            var field = AuditLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(log, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return log;
    }
}
