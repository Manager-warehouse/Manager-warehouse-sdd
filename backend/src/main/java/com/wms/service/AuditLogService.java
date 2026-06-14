package com.wms.service;

import com.wms.dto.response.AuditLogDetailResponse;
import com.wms.dto.response.AuditLogListItemResponse;
import com.wms.dto.response.AuditLogPageResponse;
import com.wms.entity.AuditLog;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.repository.AuditLogRepository;
import com.wms.repository.UserRepository;
import com.wms.util.AuditLogUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditLogService.class);

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 30;
    private static final int MAX_UNFILTERED_PAGE = 50;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           HttpServletRequest httpServletRequest) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.httpServletRequest = httpServletRequest;
    }

    @Transactional
    public void log(AuditAction action,
                    String entityType,
                    Long entityId,
                    String description,
                    Long warehouseId,
                    Map<String, Object> oldValue,
                    Map<String, Object> newValue) {
        saveAuditLog(resolveCurrentActor(), action, entityType, entityId, description,
                warehouseId, oldValue, newValue);
    }

    @Transactional
    public void log(User actor,
                    AuditAction action,
                    String entityType,
                    Long entityId,
                    String entityCode,
                    Long warehouseId,
                    Map<String, Object> oldValue,
                    Map<String, Object> newValue) {
        String description = AuditLogUtil.generateDescription(
                action, entityType, entityCode);
        saveAuditLog(actor, action, entityType, entityId, description,
                warehouseId, oldValue, newValue);
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse getAuditLogs(
            Integer page,
            Integer pageSize,
            String from,
            String to,
            Long warehouseId) {

        int resolvedPage = resolvePage(page);
        int resolvedPageSize = resolvePageSize(pageSize);
        OffsetDateTime fromTime = parseBoundary(from, true);
        OffsetDateTime toTime = parseBoundary(to, false);
        validateDateRange(fromTime, toTime);

        boolean hasFilter = fromTime != null || toTime != null || warehouseId != null;
        if (!hasFilter && resolvedPage > MAX_UNFILTERED_PAGE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "QUERY_RANGE_TOO_LARGE");
        }

        Page<AuditLog> result = auditLogRepository.findAll(
                buildSpecification(fromTime, toTime, warehouseId),
                PageRequest.of(resolvedPage - 1, resolvedPageSize,
                        Sort.by(Sort.Direction.DESC, "timestamp")));

        return buildPageResponse(result, resolvedPage, resolvedPageSize, !hasFilter);
    }

    @Transactional(readOnly = true)
    public AuditLogDetailResponse getAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AUDIT_LOG_NOT_FOUND"));
        return AuditLogDetailResponse.from(auditLog);
    }

    private void saveAuditLog(User actor,
                              AuditAction action,
                              String entityType,
                              Long entityId,
                              String description,
                              Long warehouseId,
                              Map<String, Object> oldValue,
                              Map<String, Object> newValue) {
        if (actor == null) {
            throw new IllegalStateException("Audit actor is required");
        }

        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setActorRole(actor.getRole().name());
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(description);
        entry.setWarehouse(toWarehouseReference(warehouseId));
        entry.setOldValue(AuditLogUtil.toJson(
                AuditLogUtil.filterSensitiveFields(oldValue)));
        entry.setNewValue(AuditLogUtil.toJson(
                AuditLogUtil.filterSensitiveFields(newValue)));
        entry.setIpAddress(resolveClientIp());

        auditLogRepository.save(entry);
        log.debug("Audit log created: {} {} {} (actor={})",
                action, entityType, entityId, actor.getId());
    }

    private Specification<AuditLog> buildSpecification(
            OffsetDateTime from,
            OffsetDateTime to,
            Long warehouseId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(
                        root.get("timestamp"), to));
            }
            if (warehouseId != null) {
                predicates.add(builder.equal(
                        root.get("warehouse").get("id"), warehouseId));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogPageResponse buildPageResponse(
            Page<AuditLog> result,
            int page,
            int pageSize,
            boolean unfiltered) {
        List<AuditLogListItemResponse> data = result.getContent().stream()
                .map(AuditLogListItemResponse::from)
                .toList();
        boolean requiresFilter = unfiltered && page >= MAX_UNFILTERED_PAGE;
        return new AuditLogPageResponse(
                data, page, pageSize, result.hasNext(),
                result.hasPrevious(), requiresFilter);
    }

    private User resolveCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Authenticated actor is required");
        }
        // JwtAuthFilter stores a Spring UserDetails (not the WMS User entity) as principal.
        // Resolve the actual User entity by email (principal name).
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Audit actor not found: " + email));
    }

    private Warehouse toWarehouseReference(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        return warehouse;
    }

    private int resolvePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private OffsetDateTime parseBoundary(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return parseDateBoundary(value, startOfDay);
        }
    }

    private OffsetDateTime parseDateBoundary(String value, boolean startOfDay) {
        try {
            LocalDate date = LocalDate.parse(value);
            if (startOfDay) {
                return date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            }
            return date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                    .toOffsetDateTime().minusNanos(1);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
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
