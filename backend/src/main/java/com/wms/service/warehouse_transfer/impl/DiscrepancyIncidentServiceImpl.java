package com.wms.service.warehouse_transfer.impl;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.DiscrepancyIncidentRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_transfer.DiscrepancyIncidentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscrepancyIncidentServiceImpl implements DiscrepancyIncidentService {

    /*
     * LUỒNG HỒ SƠ CHÊNH LỆCH ĐIỀU CHUYỂN:
     * - Các hàm public là hành động chính: xem danh sách incident và xử lý/đóng incident.
     * - Các hàm private là hàm hỗ trợ: kiểm quyền xem/xử lý, snapshot audit và kiểm chuỗi rỗng.
     */
    private static final String OPEN = "OPEN";
    private static final Set<String> RESOLUTION_STATUSES = Set.of(
            "RESOLVED_ACCEPTED",
            "RESOLVED_SOURCE_FAULT",
            "RESOLVED_CARRIER_FAULT",
            "RESOLVED_DESTINATION_COUNT_ERROR"
    );

    private final DiscrepancyIncidentRepository incidentRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public DiscrepancyIncidentServiceImpl(DiscrepancyIncidentRepository incidentRepository,
                                          UserWarehouseAssignmentRepository assignmentRepository,
                                          AuditLogService auditLogService) {
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyIncidentResponse> listIncidents(String status, User actor) {
        // HÀM CHÍNH: liệt kê hồ sơ chênh lệch mà người dùng được phép xem.
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<DiscrepancyIncident> incidents = isBlank(status)
                ? incidentRepository.findAllWithDetails(sort)
                : incidentRepository.findByStatus(status.trim(), sort);

        return incidents.stream()
                .filter(incident -> canAccess(incident, actor))
                .map(DiscrepancyIncidentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public DiscrepancyIncidentResponse resolveIncident(Long id,
                                                       DiscrepancyIncidentResolveRequest request,
                                                       User actor) {
        // HÀM CHÍNH: người có quyền kết luận và đóng hồ sơ chênh lệch.
        DiscrepancyIncident incident = incidentRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DISCREPANCY_INCIDENT_NOT_FOUND"));

        if (!canAccess(incident, actor) || !canResolve(actor)) {
            throw new BusinessRuleViolationException("DISCREPANCY_INCIDENT_ACCESS_DENIED");
        }
        if (!OPEN.equals(incident.getStatus())) {
            throw new BusinessRuleViolationException("DISCREPANCY_INCIDENT_NOT_OPEN");
        }
        String resolutionStatus = request.status().trim();
        if (!RESOLUTION_STATUSES.contains(resolutionStatus)) {
            throw new BusinessRuleViolationException("DISCREPANCY_RESOLUTION_STATUS_INVALID");
        }

        Map<String, Object> before = snapshot(incident);
        incident.setStatus(resolutionStatus);
        incident.setResolutionNote(request.resolutionNote().trim());
        incident.setResolvedBy(actor);
        incident.setResolvedAt(OffsetDateTime.now());
        DiscrepancyIncident saved = incidentRepository.save(incident);

        auditLogService.log(
                actor,
                AuditAction.STATUS_CHANGE,
                "DISCREPANCY_INCIDENT",
                saved.getId(),
                saved.getIncidentType() + "-" + saved.getId(),
                saved.getTransfer().getDestinationWarehouse().getId(),
                before,
                snapshot(saved)
        );

        return DiscrepancyIncidentResponse.from(saved);
    }

    private boolean canAccess(DiscrepancyIncident incident, User actor) {
        // HÀM HỖ TRỢ: kiểm người dùng có được xem incident theo vai trò và kho liên quan không.
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        UserRole role = actor.getRole();
        if (role == UserRole.ADMIN || role == UserRole.CEO || role == UserRole.ACCOUNTANT_MANAGER) {
            return true;
        }
        if (role != UserRole.WAREHOUSE_MANAGER) {
            return false;
        }
        List<Long> allowedWarehouseIds = assignmentRepository.findWarehouseIdsByUserId(actor.getId());
        InterWarehouseTransfer transfer = incident.getTransfer();
        return allowedWarehouseIds.contains(transfer.getSourceWarehouse().getId())
                || allowedWarehouseIds.contains(transfer.getDestinationWarehouse().getId());
    }

    private boolean canResolve(User actor) {
        // HÀM HỖ TRỢ: kiểm vai trò được phép xử lý/kết luận incident.
        if (actor == null || actor.getRole() == null) {
            return false;
        }
        return actor.getRole() == UserRole.ADMIN
                || actor.getRole() == UserRole.CEO
                || actor.getRole() == UserRole.WAREHOUSE_MANAGER
                || actor.getRole() == UserRole.ACCOUNTANT_MANAGER;
    }

    private Map<String, Object> snapshot(DiscrepancyIncident incident) {
        // HÀM HỖ TRỢ: lấy trạng thái trước/sau để ghi audit log khi xử lý incident.
        return Map.of(
                "status", incident.getStatus(),
                "resolutionNote", incident.getResolutionNote() == null ? "" : incident.getResolutionNote(),
                "resolvedById", incident.getResolvedBy() == null ? "" : incident.getResolvedBy().getId(),
                "resolvedAt", incident.getResolvedAt() == null ? "" : incident.getResolvedAt().toString()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
