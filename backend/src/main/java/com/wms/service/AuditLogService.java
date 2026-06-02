package com.wms.service;

import com.wms.dto.AuditLogPageResponse;
import com.wms.dto.AuditLogResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.repository.AuditLogRepository;
import com.wms.util.AuditLogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditLogService.class);

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_DATE_RANGE_DAYS = 7;

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest httpServletRequest;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           HttpServletRequest httpServletRequest) {
        this.auditLogRepository = auditLogRepository;
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * Creates an immutable audit log entry for a warehouse operation.
     * Called by other services after business operations complete.
     */
    @Transactional
    public void log(User actor,
                    AuditAction action,
                    String entityType,
                    Long entityId,
                    String entityCode,
                    Long warehouseId,
                    Map<String, Object> oldValue,
                    Map<String, Object> newValue) {

        Map<String, Object> filteredOld =
                AuditLogUtil.filterSensitiveFields(oldValue);
        Map<String, Object> filteredNew =
                AuditLogUtil.filterSensitiveFields(newValue);

        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setActorRole(actor.getRole().name());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(AuditLogUtil.generateDescription(
                action, entityType, entityCode));

        if (warehouseId != null) {
            Warehouse wh = new Warehouse();
            wh.setId(warehouseId);
            entry.setWarehouse(wh);
        }

        entry.setOldValue(AuditLogUtil.toJson(filteredOld));
        entry.setNewValue(AuditLogUtil.toJson(filteredNew));
        entry.setIpAddress(resolveClientIp());

        auditLogRepository.save(entry);

        log.debug("Audit log created: {} {} {} (actor={})",
                action, entityType, entityCode, actor.getId());
    }

    /**
     * Queries audit logs with cursor-based pagination and filters.
     * RBAC is enforced: WAREHOUSE_MANAGER sees only assigned warehouses.
     */
    @Transactional(readOnly = true)
    public AuditLogPageResponse getAuditLogs(
            Long cursor,
            Integer size,
            Long actorId,
            String entityType,
            String action,
            Long warehouseId,
            LocalDate startDate,
            LocalDate endDate,
            User currentUser,
            List<Long> assignedWarehouseIds) {

        int pageSize = resolvePageSize(size);
        OffsetDateTime start = resolveStartTimestamp(startDate);
        OffsetDateTime end = resolveEndTimestamp(endDate);

        boolean isWarehouseScoped = isWarehouseScoped(currentUser);

        List<AuditLog> results = fetchAuditLogs(
                cursor, pageSize, actorId, entityType, action,
                warehouseId, start, end,
                isWarehouseScoped, assignedWarehouseIds);

        return buildPageResponse(results, pageSize);
    }

    private List<AuditLog> fetchAuditLogs(
            Long cursor, int pageSize,
            Long actorId, String entityType, String action,
            Long warehouseId,
            OffsetDateTime start, OffsetDateTime end,
            boolean isWarehouseScoped,
            List<Long> assignedWarehouseIds) {

        Pageable pageable = PageRequest.of(0, pageSize + 1);

        if (isWarehouseScoped) {
            return fetchWarehouseScopedLogs(
                    cursor, actorId, entityType, action,
                    start, end, assignedWarehouseIds, pageable);
        }

        return fetchAllLogs(
                cursor, actorId, entityType, action,
                warehouseId, start, end, pageable);
    }

    private List<AuditLog> fetchAllLogs(
            Long cursor, Long actorId, String entityType,
            String action, Long warehouseId,
            OffsetDateTime start, OffsetDateTime end,
            Pageable pageable) {

        if (cursor == null) {
            return auditLogRepository.findByFilters(
                    start, end, actorId, entityType,
                    action, warehouseId, pageable);
        }
        return auditLogRepository.findByCursorAndFilters(
                cursor, start, end, actorId, entityType,
                action, warehouseId, pageable);
    }

    private List<AuditLog> fetchWarehouseScopedLogs(
            Long cursor, Long actorId, String entityType,
            String action, OffsetDateTime start, OffsetDateTime end,
            List<Long> assignedWarehouseIds, Pageable pageable) {

        if (cursor == null) {
            return auditLogRepository.findByFiltersAndWarehouseIds(
                    start, end, actorId, entityType,
                    action, assignedWarehouseIds, pageable);
        }
        return auditLogRepository
                .findByCursorAndFiltersAndWarehouseIds(
                        cursor, start, end, actorId, entityType,
                        action, assignedWarehouseIds, pageable);
    }

    private AuditLogPageResponse buildPageResponse(
            List<AuditLog> results, int pageSize) {

        boolean hasNext = results.size() > pageSize;

        List<AuditLog> pageData = hasNext
                ? results.subList(0, pageSize)
                : results;

        Long nextCursor = hasNext
                ? pageData.get(pageData.size() - 1).getId()
                : null;

        List<AuditLogResponse> data = pageData.stream()
                .map(AuditLogResponse::from)
                .toList();

        return new AuditLogPageResponse(data, nextCursor, hasNext);
    }

    private boolean isWarehouseScoped(User user) {
        return user.getRole() == UserRole.WAREHOUSE_MANAGER;
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private OffsetDateTime resolveStartTimestamp(LocalDate startDate) {
        LocalDate date = (startDate != null)
                ? startDate
                : LocalDate.now().minusDays(DEFAULT_DATE_RANGE_DAYS);
        return date.atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
    }

    private OffsetDateTime resolveEndTimestamp(LocalDate endDate) {
        LocalDate date = (endDate != null)
                ? endDate
                : LocalDate.now();
        return date.atTime(LocalTime.of(23, 59, 59))
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
    }

    private String resolveClientIp() {
        try {
            String xForwardedFor =
                    httpServletRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Could not resolve client IP", e);
            return null;
        }
    }
}
