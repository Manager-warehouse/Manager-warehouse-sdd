package com.wms.service.warehouse_transfer.impl;

import com.wms.dto.request.DiscrepancyIncidentResolveRequest;
import com.wms.dto.response.DiscrepancyIncidentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.entity.warehouse_transfer.DiscrepancyHoldEntry;
import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferAllocation;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferItem;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_control.AdjustmentStatus;
import com.wms.enums.stock_control.AdjustmentType;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DiscrepancyHoldEntryRepository;
import com.wms.repository.DiscrepancyIncidentRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InterWarehouseTransferAllocationRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_transfer.DiscrepancyIncidentService;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
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
            "RESOLVED_SOURCE_FAULT",
            "RESOLVED_DESTINATION_COUNT_ERROR"
    );

    private final DiscrepancyIncidentRepository incidentRepository;
    private final DiscrepancyHoldEntryRepository holdEntryRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository locationRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final AuditLogService auditLogService;
    private final PartnerAuditUtil auditUtil;
    private final InterWarehouseTransferHelper transferHelper;

    public DiscrepancyIncidentServiceImpl(DiscrepancyIncidentRepository incidentRepository,
                                          DiscrepancyHoldEntryRepository holdEntryRepository,
                                          InterWarehouseTransferAllocationRepository allocationRepository,
                                          InventoryRepository inventoryRepository,
                                          WarehouseLocationRepository locationRepository,
                                          AdjustmentRepository adjustmentRepository,
                                          AuditLogService auditLogService,
                                          PartnerAuditUtil auditUtil,
                                          InterWarehouseTransferHelper transferHelper) {
        this.incidentRepository = incidentRepository;
        this.holdEntryRepository = holdEntryRepository;
        this.allocationRepository = allocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.auditLogService = auditLogService;
        this.auditUtil = auditUtil;
        this.transferHelper = transferHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscrepancyIncidentResponse> listIncidents(String status, User actor) {
        // HÀM CHÍNH: chỉ CEO được xem hồ sơ chênh lệch vì đây là bước kết luận trách nhiệm cuối.
        requireCeo(actor);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<DiscrepancyIncident> incidents = isBlank(status)
                ? incidentRepository.findAllWithDetails(sort)
                : incidentRepository.findByStatus(status.trim(), sort);

        return incidents.stream()
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

        requireCeo(actor);
        if (!OPEN.equals(incident.getStatus())) {
            throw new BusinessRuleViolationException("DISCREPANCY_INCIDENT_NOT_OPEN");
        }
        String resolutionStatus = request.status().trim();
        if (!RESOLUTION_STATUSES.contains(resolutionStatus)) {
            throw new BusinessRuleViolationException("DISCREPANCY_RESOLUTION_STATUS_INVALID");
        }

        Map<String, Object> before = snapshot(incident);
        if ("OVER_RECEIPT".equals(incident.getIncidentType())
                && "RESOLVED_SOURCE_FAULT".equals(resolutionStatus)) {
            applySourceFaultOverReceipt(incident, request.resolutionNote().trim(), actor);
        }
        if ("SHORTAGE".equals(incident.getIncidentType())
                && "RESOLVED_DESTINATION_COUNT_ERROR".equals(resolutionStatus)) {
            applyDestinationCountErrorShortage(incident, request.resolutionNote().trim(), actor);
        }
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

    private void requireCeo(User actor) {
        // Hồ sơ chênh lệch là quyết định quy trách nhiệm, nên chỉ CEO được xem và xử lý.
        if (actor == null || actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("DISCREPANCY_INCIDENT_ACCESS_DENIED");
        }
    }

    private void applySourceFaultOverReceipt(DiscrepancyIncident incident, String reason, User actor) {
        // CEO kết luận kho nguồn gửi thừa: phần đang tạm giữ được nhập thật vào kho đích,
        // đồng thời trừ thêm đúng số lượng đó khỏi kho nguồn để tổng tồn hệ thống không tăng ảo.
        List<DiscrepancyHoldEntry> holds = holdEntryRepository.findByIncidentId(incident.getId());
        if (holds.isEmpty()) {
            throw new BusinessRuleViolationException("DISCREPANCY_HOLD_ENTRY_NOT_FOUND");
        }
        BigDecimal heldQty = holds.stream()
                .map(DiscrepancyHoldEntry::getHoldQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (heldQty.compareTo(incident.getQuantity()) != 0) {
            throw new BusinessRuleViolationException("DISCREPANCY_HOLD_QUANTITY_MISMATCH");
        }

        Warehouse sourceWarehouse = incident.getTransfer().getSourceWarehouse();
        BigDecimal remainingToDeduct = heldQty;
        List<Inventory> sourceRows = inventoryRepository.findReservableForUpdate(
                sourceWarehouse.getId(), incident.getProduct().getId());
        BigDecimal sourceAvailable = sourceRows.stream()
                .map(inventory -> inventory.getTotalQty().subtract(inventory.getReservedQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sourceAvailable.compareTo(heldQty) < 0) {
            throw new BusinessRuleViolationException("SOURCE_STOCK_NOT_ENOUGH_FOR_DISCREPANCY_RESOLUTION");
        }

        for (Inventory source : sourceRows) {
            if (remainingToDeduct.signum() <= 0) {
                break;
            }
            BigDecimal available = source.getTotalQty().subtract(source.getReservedQty());
            if (available.signum() <= 0) {
                continue;
            }
            BigDecimal deducted = available.min(remainingToDeduct);
            BigDecimal beforeQty = source.getTotalQty();
            source.setTotalQty(beforeQty.subtract(deducted));
            source.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(source);
            auditInventory(actor, source, beforeQty, source.getTotalQty(), deducted.negate(), reason);
            createApprovedAdjustment(incident, sourceWarehouse, source.getLocation(), source.getBatch(),
                    deducted.negate(), reason, actor);
            remainingToDeduct = remainingToDeduct.subtract(deducted);
        }

        for (DiscrepancyHoldEntry hold : holds) {
            WarehouseLocation location = hold.getHoldLocation();
            if (location == null || hold.getBatch() == null) {
                throw new BusinessRuleViolationException("DISCREPANCY_HOLD_ENTRY_INCOMPLETE");
            }
            BigDecimal beforeDestinationQty = inventoryRepository.findByStockKeyForUpdate(
                            hold.getWarehouse().getId(), hold.getProduct().getId(),
                            hold.getBatch().getId(), location.getId())
                    .map(Inventory::getTotalQty)
                    .orElse(BigDecimal.ZERO);
            applyLocationOccupancy(location, hold.getProduct(), hold.getHoldQty());
            transferHelper.upsertInventory(hold.getWarehouse(), hold.getProduct(), hold.getBatch(),
                    location, hold.getHoldQty(), BigDecimal.ZERO);
            Inventory destination = inventoryRepository.findByStockKeyForUpdate(
                            hold.getWarehouse().getId(), hold.getProduct().getId(),
                            hold.getBatch().getId(), location.getId())
                    .orElseThrow(() -> new BusinessRuleViolationException("INVENTORY_ROW_NOT_FOUND"));
            auditInventory(actor, destination, beforeDestinationQty, destination.getTotalQty(), hold.getHoldQty(), reason);
            createApprovedAdjustment(incident, hold.getWarehouse(), location, hold.getBatch(),
                    hold.getHoldQty(), reason, actor);
        }
    }

    private void applyDestinationCountErrorShortage(DiscrepancyIncident incident, String reason, User actor) {
        // CEO kết luận kho đích đếm thiếu: phần thiếu thực tế vẫn ở kho đích,
        // nên bù lại vào đúng kệ nhận hàng và tạo adjustment dương để tổng tồn quay về đúng.
        InterWarehouseTransferItem item = transferHelper.items(incident.getTransfer()).stream()
                .filter(row -> row.getProduct().getId().equals(incident.getProduct().getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("TRANSFER_ITEM_NOT_FOUND"));
        WarehouseLocation destinationLocation = item.getDestinationLocation();
        if (destinationLocation == null) {
            throw new BusinessRuleViolationException("DESTINATION_LOCATION_REQUIRED");
        }
        List<InterWarehouseTransferAllocation> allocations = allocationRepository.findByTransferItemId(item.getId());
        if (allocations.isEmpty() || allocations.get(0).getInventory() == null
                || allocations.get(0).getInventory().getBatch() == null) {
            throw new BusinessRuleViolationException("TRANSFER_ALLOCATION_NOT_FOUND");
        }

        var batch = allocations.get(0).getInventory().getBatch();
        BigDecimal beforeDestinationQty = inventoryRepository.findByStockKeyForUpdate(
                        incident.getTransfer().getDestinationWarehouse().getId(),
                        incident.getProduct().getId(),
                        batch.getId(),
                        destinationLocation.getId())
                .map(Inventory::getTotalQty)
                .orElse(BigDecimal.ZERO);
        applyLocationOccupancy(destinationLocation, incident.getProduct(), incident.getQuantity());
        transferHelper.upsertInventory(incident.getTransfer().getDestinationWarehouse(), incident.getProduct(), batch,
                destinationLocation, incident.getQuantity(), BigDecimal.ZERO);
        Inventory destination = inventoryRepository.findByStockKeyForUpdate(
                        incident.getTransfer().getDestinationWarehouse().getId(),
                        incident.getProduct().getId(),
                        batch.getId(),
                        destinationLocation.getId())
                .orElseThrow(() -> new BusinessRuleViolationException("INVENTORY_ROW_NOT_FOUND"));
        auditInventory(actor, destination, beforeDestinationQty, destination.getTotalQty(), incident.getQuantity(), reason);
        createApprovedAdjustment(incident, incident.getTransfer().getDestinationWarehouse(), destinationLocation, batch,
                incident.getQuantity(), reason, actor);
    }

    private void applyLocationOccupancy(WarehouseLocation location, Product product, BigDecimal qty) {
        BigDecimal addedVolume = product.getVolumeM3() != null ? product.getVolumeM3().multiply(qty) : BigDecimal.ZERO;
        BigDecimal addedWeight = product.getWeightKg() != null ? product.getWeightKg().multiply(qty) : BigDecimal.ZERO;
        BigDecimal currentVolume = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal currentWeight = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;
        location.setCurrentVolumeM3(currentVolume.add(addedVolume));
        location.setCurrentWeightKg(currentWeight.add(addedWeight));
        location.setUpdatedAt(OffsetDateTime.now());
        locationRepository.save(location);
    }

    private void createApprovedAdjustment(DiscrepancyIncident incident,
                                          Warehouse warehouse,
                                          WarehouseLocation location,
                                          com.wms.entity.stock_control.Batch batch,
                                          BigDecimal qty,
                                          String reason,
                                          User actor) {
        Adjustment adjustment = Adjustment.builder()
                .adjustmentNumber("ADJ-DIS-" + incident.getId() + "-" + System.nanoTime())
                .warehouse(warehouse)
                .product(incident.getProduct())
                .batch(batch)
                .location(location)
                .quantityAdjustment(qty)
                .type(AdjustmentType.TRANSFER_DISCREPANCY)
                .status(AdjustmentStatus.APPROVED)
                .referenceId(incident.getId())
                .referenceType(qty.signum() < 0 ? "TRANSFER_DISCREPANCY_SOURCE" : "TRANSFER_DISCREPANCY_DESTINATION")
                .reason(reason)
                .approvedBy(actor)
                .approvedAt(OffsetDateTime.now())
                .documentDate(incident.getTransfer().getDocumentDate() != null
                        ? incident.getTransfer().getDocumentDate()
                        : LocalDate.now())
                .accountingPeriod(incident.getTransfer().getAccountingPeriod())
                .createdBy(actor)
                .createdAt(OffsetDateTime.now())
                .build();
        adjustmentRepository.save(adjustment);
    }

    private void auditInventory(User actor, Inventory inventory, BigDecimal beforeQty,
                                BigDecimal afterQty, BigDecimal delta, String reason) {
        auditUtil.logChange(actor, AuditAction.INVENTORY_UPDATE, "INVENTORY",
                inventory.getId(), "INV-" + inventory.getWarehouse().getId() + "-" + inventory.getProduct().getId(),
                Map.of("totalQty", beforeQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", afterQty, "reservedQty", inventory.getReservedQty(),
                        "delta", delta, "reason", reason));
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
