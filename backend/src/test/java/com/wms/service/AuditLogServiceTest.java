package com.wms.service;

import com.wms.dto.response.AuditLogDetailResponse;
import com.wms.dto.response.AuditLogPageResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    private User adminActor;

    @BeforeEach
    void setUp() {
        adminActor = new User();
        adminActor.setId(1L);
        adminActor.setFullName("System Admin");
        adminActor.setEmail("admin@wms.com");
        adminActor.setRole(UserRole.ADMIN);
        SecurityContextHolder.clearContext();
    }

    // ─── LOG CREATION ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Ghi audit log đầy đủ các field khi có actor rõ ràng")
    void log_withExplicitActor_savesAllFields() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        auditLogService.log(
                adminActor, AuditAction.UPDATE, "User", 101L, "U101",
                2L,
                Map.of("fullName", "Old Name"),
                Map.of("fullName", "New Name"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActor()).isEqualTo(adminActor);
        assertThat(saved.getActorRole()).isEqualTo("ADMIN");
        assertThat(saved.getAction()).isEqualTo(AuditAction.UPDATE);
        assertThat(saved.getEntityType()).isEqualTo("User");
        assertThat(saved.getEntityId()).isEqualTo(101L);
        assertThat(saved.getDescription()).isEqualTo("UPDATE User U101");
        assertThat(saved.getWarehouse().getId()).isEqualTo(2L);
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getOldValue()).contains("Old Name");
        assertThat(saved.getNewValue()).contains("New Name");
    }

    @Test
    @DisplayName("Ghi audit log từ SecurityContext khi không truyền actor")
    void log_withSecurityContext_resolvesActorFromContext() {
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");

        // Đặt adminActor vào SecurityContext
        var auth = new UsernamePasswordAuthenticationToken(
                adminActor, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        auditLogService.log(
                AuditAction.LOGIN, "User", 1L, "LOGIN User admin@wms.com",
                null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo(adminActor);
        assertThat(captor.getValue().getActorRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Throw khi log với actor null")
    void log_nullActor_throwsIllegalState() {
        assertThatThrownBy(() -> auditLogService.log(
                null, AuditAction.CREATE, "User", 1L, "1",
                null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Audit actor is required");
    }

    @Test
    @DisplayName("Không lưu password, token vào audit log (sensitive field exclusion)")
    void log_sensitiveFields_areExcluded() {
        auditLogService.log(
                adminActor, AuditAction.UPDATE, "User", 1L, "1",
                null,
                Map.of("passwordHash", "old-hash", "email", "old@wms.com"),
                Map.of("passwordHash", "new-hash", "email", "new@wms.com"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getOldValue()).doesNotContain("passwordHash").doesNotContain("old-hash");
        assertThat(saved.getNewValue()).doesNotContain("passwordHash").doesNotContain("new-hash");
        assertThat(saved.getOldValue()).contains("old@wms.com");
        assertThat(saved.getNewValue()).contains("new@wms.com");
        assertThat(saved.getWarehouse()).isNull();
    }

    @Test
    @DisplayName("Không lưu accessToken, refreshToken, token vào audit log")
    void log_tokenFields_areExcluded() {
        auditLogService.log(
                adminActor, AuditAction.LOGIN, "User", 1L, "1",
                null,
                null,
                Map.of("accessToken", "jwt-abc", "refreshToken", "rf-xyz", "email", "u@wms.com"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getNewValue())
                .doesNotContain("accessToken")
                .doesNotContain("refreshToken")
                .doesNotContain("jwt-abc")
                .contains("u@wms.com");
    }

    @Test
    @DisplayName("Ghi IP từ X-Forwarded-For header khi có proxy")
    void log_xForwardedFor_usesFirstIp() {
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");

        auditLogService.log(
                adminActor, AuditAction.CREATE, "Product", 5L, "P001",
                null, null, Map.of("name", "Test Product"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.5");
    }

    // ─── GET AUDIT LOGS (PAGINATION) ─────────────────────────────────────────

    @Test
    @DisplayName("Trả về trang 1 mặc định với 30 entries khi không có filter")
    void getAuditLogs_noParams_returnsDefaultPage1Size30() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAuditLog(1L))));

        AuditLogPageResponse response = auditLogService.getAuditLogs(null, null, null, null, null);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getPageSize()).isEqualTo(30);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("pageSize vượt quá 30 → bị cap về 30")
    void getAuditLogs_pageSizeOver30_isCappedAt30() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AuditLogPageResponse response = auditLogService.getAuditLogs(1, 100, null, null, null);

        assertThat(response.getPageSize()).isEqualTo(30);
    }

    @Test
    @DisplayName("page null hoặc < 1 → mặc định về trang 1")
    void getAuditLogs_invalidPage_defaultsToPage1() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AuditLogPageResponse r1 = auditLogService.getAuditLogs(null, null, null, null, null);
        AuditLogPageResponse r2 = auditLogService.getAuditLogs(0, null, null, null, null);

        assertThat(r1.getPage()).isEqualTo(1);
        assertThat(r2.getPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("Yêu cầu trang > 50 không có filter → 400 QUERY_RANGE_TOO_LARGE")
    void getAuditLogs_page51NoFilter_throwsQueryRangeTooLarge() {
        ResponseStatusException ex = catchThrowableOfType(
                () -> auditLogService.getAuditLogs(51, 30, null, null, null),
                ResponseStatusException.class);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("QUERY_RANGE_TOO_LARGE");
    }

    @Test
    @DisplayName("Trang > 50 nhưng có filter time → được phép (không throw)")
    void getAuditLogs_page51WithFilter_allowed() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> auditLogService.getAuditLogs(
                51, 30, "2026-01-01", "2026-12-31", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Trang > 50 nhưng có filter warehouseId → được phép")
    void getAuditLogs_page51WithWarehouseFilter_allowed() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> auditLogService.getAuditLogs(51, 30, null, null, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("from > to → 400 INVALID_DATE_RANGE")
    void getAuditLogs_fromAfterTo_throwsInvalidDateRange() {
        ResponseStatusException ex = catchThrowableOfType(
                () -> auditLogService.getAuditLogs(1, 30, "2026-12-31", "2026-01-01", null),
                ResponseStatusException.class);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    @DisplayName("Định dạng ngày không hợp lệ → 400 INVALID_DATE_RANGE")
    void getAuditLogs_invalidDateFormat_throwsInvalidDateRange() {
        ResponseStatusException ex = catchThrowableOfType(
                () -> auditLogService.getAuditLogs(1, 30, "not-a-date", null, null),
                ResponseStatusException.class);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getReason()).isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    @DisplayName("Filter theo ngày hợp lệ → trả kết quả đúng thứ tự timestamp DESC")
    void getAuditLogs_withDateFilter_returnsResults() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildAuditLog(5L), buildAuditLog(3L))));

        AuditLogPageResponse response = auditLogService.getAuditLogs(
                1, 30, "2026-01-01", "2026-12-31", null);

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getId()).isEqualTo(5L);
    }

    // ─── GET AUDIT LOG BY ID ──────────────────────────────────────────────────

    @Test
    @DisplayName("Lấy chi tiết audit log theo ID thành công")
    void getAuditLogById_found_returnsDetail() {
        when(auditLogRepository.findById(10L)).thenReturn(Optional.of(buildAuditLog(10L)));

        AuditLogDetailResponse response = auditLogService.getAuditLogById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getActorName()).isEqualTo("System Admin");
        assertThat(response.getAction()).isEqualTo("UPDATE");
        assertThat(response.getEntityType()).isEqualTo("User");
    }

    @Test
    @DisplayName("ID không tồn tại → 404 AUDIT_LOG_NOT_FOUND")
    void getAuditLogById_notFound_throws404() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = catchThrowableOfType(
                () -> auditLogService.getAuditLogById(999L),
                ResponseStatusException.class);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getReason()).isEqualTo("AUDIT_LOG_NOT_FOUND");
    }

    @Test
    @DisplayName("Chi tiết audit log có oldValue và newValue đúng")
    void getAuditLogById_returnsOldAndNewValues() {
        AuditLog entry = buildAuditLog(20L);
        entry.setOldValue("{\"status\":\"PENDING\"}");
        entry.setNewValue("{\"status\":\"APPROVED\"}");
        when(auditLogRepository.findById(20L)).thenReturn(Optional.of(entry));

        AuditLogDetailResponse response = auditLogService.getAuditLogById(20L);

        assertThat(response.getOldValue()).containsEntry("status", "PENDING");
        assertThat(response.getNewValue()).containsEntry("status", "APPROVED");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private AuditLog buildAuditLog(Long id) {
        AuditLog entry = new AuditLog();
        entry.setActor(adminActor);
        entry.setActorRole("ADMIN");
        entry.setAction(AuditAction.UPDATE);
        entry.setEntityType("User");
        entry.setEntityId(1L);
        entry.setDescription("UPDATE User 1");
        entry.setTimestamp(OffsetDateTime.now());
        entry.setOldValue("{\"fullName\":\"Old\"}");
        entry.setNewValue("{\"fullName\":\"New\"}");
        try {
            var field = AuditLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entry, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return entry;
    }
}
