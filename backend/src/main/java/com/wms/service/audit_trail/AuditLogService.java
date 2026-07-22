package com.wms.service.audit_trail;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.response.AuditLogDetailResponse;
import com.wms.dto.response.AuditLogListItemResponse;
import com.wms.dto.response.AuditLogPageResponse;
import com.wms.entity.audit_trail.AuditLog;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
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
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
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
    private final org.springframework.beans.factory.ObjectProvider<HttpServletRequest> httpServletRequestProvider;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           org.springframework.beans.factory.ObjectProvider<HttpServletRequest> httpServletRequestProvider) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.httpServletRequestProvider = httpServletRequestProvider;
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
        if (actor == null) {
            throw new IllegalStateException("Audit actor is required");
        }
        if (action == null) {
            throw new IllegalArgumentException("Audit action is required");
        }
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

        boolean hasTimeFilter = fromTime != null || toTime != null;
        boolean hasFilter = hasTimeFilter || warehouseId != null;
        if (!hasTimeFilter && resolvedPage > MAX_UNFILTERED_PAGE) {
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
                AuditLogUtil.filterSensitiveFields(normalizeChangedFields(oldValue))));
        entry.setNewValue(AuditLogUtil.toJson(
                AuditLogUtil.filterSensitiveFields(normalizeChangedFields(newValue))));
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
                data, page, pageSize, result.getTotalElements(), result.getTotalPages(), result.hasNext(),
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
        return DEFAULT_PAGE_SIZE;
    }

    private OffsetDateTime parseBoundary(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            return parseLocalDateTimeBoundary(value, startOfDay);
        }
    }

    private OffsetDateTime parseLocalDateTimeBoundary(String value, boolean startOfDay) {
        try {
            return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toOffsetDateTime();
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
                    .toOffsetDateTime();
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
        if (from != null && to != null) {
            Duration duration = Duration.between(from, to);
            if (duration.compareTo(Duration.ofHours(1)) < 0
                    || duration.toMinutes() % 60 != 0
                    || duration.getSeconds() % 3600 != 0
                    || duration.getNano() != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
            }
        }
    }

    private Map<String, Object> normalizeChangedFields(Map<String, Object> values) {
        if (values == null) {
            return Collections.emptyMap();
        }
        return values;
    }

    private String resolveClientIp() {
        try {
            HttpServletRequest request = httpServletRequestProvider.getIfAvailable();
            if (request == null) {
                return null;
            }
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("Could not resolve client IP", e);
            return null;
        }
    }
}
