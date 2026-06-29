package com.wms.service.transfer.impl;

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterWarehouseTransferReceivingService {

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final PartnerAuditUtil auditUtil;
    private final InterWarehouseTransferHelper helper;
    private final QuarantineRecordRepository quarantineRecordRepository;

    @Transactional
    public InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferReceiveCountItemRequest line : request.items()) {
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            if (line.receivedQty().compareTo(item.getSentQty()) > 0) {
                throw new BusinessRuleViolationException("OVER_RECEIPT_BLOCKED");
            }
            if (line.receivedQty().compareTo(item.getSentQty()) != 0 && helper.isBlank(line.issueReason())) {
                throw new BusinessRuleViolationException("ISSUE_REASON_REQUIRED");
            }
            item.setWorkerReceivedQty(line.receivedQty());
            item.setIssueReason(line.issueReason());
            transferItemRepository.save(item);
        }
        helper.audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_COUNT, before, helper.snapshot(transfer));
        return helper.toResponse(transfer);
    }

    @Transactional
    public InterWarehouseTransferResponse receiveCheck(Long id, InterWarehouseTransferReceiveCheckRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferReceiveCheckItemRequest line : request.items()) {
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
            validateReceiveCheckLine(transfer, item, line);
            item.setReceivedQty(line.confirmedQty());
            item.setQcPassedQty(line.qcPassedQty());
            item.setQcFailedQty(line.qcFailedQty());
            item.setDestinationLocation(line.destinationLocationId() == null
                    ? null
                    : helper.reference(WarehouseLocation.class, line.destinationLocationId()));
            item.setCheckerNote(line.checkerNote());
            item.setQcFailureReason(line.qcFailureReason());
            item.setCheckedBy(actor);
            item.setCheckedAt(OffsetDateTime.now());
            item.setVarianceQty(line.confirmedQty().subtract(item.getSentQty()));
            transferItemRepository.save(item);
        }
        helper.audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_CHECK, before, helper.snapshot(transfer));
        return helper.toResponse(transfer);
    }

    @Transactional
    public InterWarehouseTransferResponse finalReceive(Long id, InterWarehouseTransferFinalReceiveRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());
        ensureAllChecked(transfer);
        boolean shortage = helper.items(transfer).stream().anyMatch(item -> item.getVarianceQty().compareTo(BigDecimal.ZERO) < 0);
        if (shortage && helper.isBlank(request.discrepancyReason())) {
            throw new BusinessRuleViolationException("DISCREPANCY_REASON_REQUIRED");
        }
        Map<String, Object> before = helper.snapshot(transfer);
        moveTransitToDestination(transfer, request, actor);
        transfer.setStatus(shortage ? InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY : InterWarehouseTransferStatus.COMPLETED);
        transfer.setDiscrepancyReason(request.discrepancyReason());
        transfer.setConfirmedBy(actor);
        transfer.setConfirmedAt(OffsetDateTime.now());
        transfer.setActualReceivedDate(OffsetDateTime.now().toLocalDate());
        transfer.setUpdatedAt(OffsetDateTime.now());
        transfer.getTrip().getDriver().setStatus(DriverStatus.AVAILABLE);
        transfer.getTrip().getVehicle().setStatus(VehicleStatus.AVAILABLE);
        transfer.getTrip().setStatus(TripStatus.COMPLETED);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_FINAL_RECEIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnToSource(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.PLANNER) {
            if (actor.getRole() != UserRole.WAREHOUSE_MANAGER) {
                throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
            }
            helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturned(true);
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse quarantineReject(Long id, InterWarehouseTransferRejectRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        helper.ensureWarehouseScope(actor, targetWarehouseId);

        if (helper.isBlank(request.getRejectionReason())) {
            throw new BusinessRuleViolationException("REJECTION_REASON_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);

        moveTransitToQuarantine(transfer, actor);

        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getSentQty() != null ? item.getSentQty() : item.getPlannedQty();
            item.setReceivedQty(qty);
            item.setQcPassedQty(BigDecimal.ZERO);
            item.setQcFailedQty(qty);
            item.setCheckerNote(request.getRejectionReason());
            item.setQcFailureReason(request.getRejectionReason());
            item.setCheckedBy(actor);
            item.setCheckedAt(OffsetDateTime.now());
            item.setVarianceQty(BigDecimal.ZERO);
            transferItemRepository.save(item);
        }

        transfer.setStatus(InterWarehouseTransferStatus.QUARANTINED);
        transfer.setRejectedBy(actor);
        transfer.setRejectedAt(OffsetDateTime.now());
        transfer.setRejectionReason(request.getRejectionReason());
        transfer.setUpdatedAt(OffsetDateTime.now());

        if (transfer.getTrip() != null) {
            transfer.getTrip().getDriver().setStatus(DriverStatus.AVAILABLE);
            transfer.getTrip().getVehicle().setStatus(VehicleStatus.AVAILABLE);
            transfer.getTrip().setStatus(TripStatus.COMPLETED);
            transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_QUARANTINE_REJECT, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void validateReceiveCheckLine(InterWarehouseTransfer transfer, InterWarehouseTransferItem item, InterWarehouseTransferReceiveCheckItemRequest line) {
        if (item.getWorkerReceivedQty() == null) {
            throw new BusinessRuleViolationException("WORKER_COUNT_REQUIRED");
        }
        if (line.confirmedQty().compareTo(item.getSentQty()) > 0) {
            throw new BusinessRuleViolationException("OVER_RECEIPT_BLOCKED");
        }
        if (line.confirmedQty().compareTo(item.getWorkerReceivedQty()) != 0 && helper.isBlank(line.checkerNote())) {
            throw new BusinessRuleViolationException("CHECKER_NOTE_REQUIRED");
        }
        if (line.qcPassedQty().add(line.qcFailedQty()).compareTo(line.confirmedQty()) != 0) {
            throw new BusinessRuleViolationException("QC_TOTAL_MUST_MATCH_CONFIRMED_QTY");
        }
        if (line.qcFailedQty().signum() > 0 && helper.isBlank(line.qcFailureReason())) {
            throw new BusinessRuleViolationException("QC_FAILURE_REASON_REQUIRED");
        }
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        if (line.qcPassedQty().signum() > 0) {
            if (line.destinationLocationId() == null) {
                throw new BusinessRuleViolationException("DESTINATION_LOCATION_REQUIRED");
            }
            WarehouseLocation destination = locationRepository.findById(line.destinationLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination location not found: " + line.destinationLocationId()));
            if (!Objects.equals(destination.getWarehouse().getId(), targetWarehouseId)
                    || Boolean.FALSE.equals(destination.getIsActive())) {
                throw new BusinessRuleViolationException("INVALID_DESTINATION_LOCATION");
            }
            if (Boolean.TRUE.equals(destination.getIsQuarantine())) {
                throw new BusinessRuleViolationException("QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE");
            }
        }
        if (line.qcFailedQty().signum() > 0) {
            boolean hasQuarantine = !locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(targetWarehouseId).isEmpty();
            if (!hasQuarantine) {
                throw new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED");
            }
        }
    }

    private void ensureAllChecked(InterWarehouseTransfer transfer) {
        if (helper.items(transfer).stream().anyMatch(item -> item.getReceivedQty() == null
                || item.getQcPassedQty() == null || item.getQcFailedQty() == null)) {
            throw new BusinessRuleViolationException("RECEIVE_CHECK_REQUIRED");
        }
    }

    private void moveTransitToDestination(InterWarehouseTransfer transfer, InterWarehouseTransferFinalReceiveRequest request, User actor) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(InterWarehouseTransferHelper.IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation quarantineLocation = null;
        Warehouse targetWarehouse = transfer.isReturned() ? transfer.getSourceWarehouse() : transfer.getDestinationWarehouse();
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal remainingPassed = helper.zero(item.getQcPassedQty());
            BigDecimal remainingFailed = helper.zero(item.getQcFailedQty());
            for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemId(item.getId())) {
                Inventory transit = inventoryRepository.findByStockKeyForUpdate(transitWarehouse.getId(),
                                item.getProduct().getId(), allocation.getInventory().getBatch().getId(),
                                helper.firstTransitLocation(transitWarehouse).getId())
                        .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_STOCK_NOT_FOUND"));
                BigDecimal qty = allocation.getAllocatedQty();
                transit.setTotalQty(transit.getTotalQty().subtract(qty));
                transit.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(transit);
                BigDecimal passQty = qty.min(remainingPassed);
                if (passQty.signum() > 0) {
                    helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                            item.getDestinationLocation(), passQty, transit.getCostPrice());
                    remainingPassed = remainingPassed.subtract(passQty);
                }
                BigDecimal failQty = qty.subtract(passQty).min(remainingFailed);
                if (failQty.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = helper.findQuarantineLocation(transfer);
                    }
                    helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                            quarantineLocation, failQty, transit.getCostPrice());
                    remainingFailed = remainingFailed.subtract(failQty);

                    // Lưu bản ghi vào quarantine_records cho hàng điều chuyển hỏng
                    QuarantineRecord qr = new QuarantineRecord();
                    qr.setWarehouse(targetWarehouse);
                    qr.setProduct(item.getProduct());
                    qr.setBatch(transit.getBatch());
                    qr.setLocation(quarantineLocation);
                    qr.setTransfer(transfer);
                    qr.setTransferItem(item);
                    qr.setOriginType("INTERNAL_TRANSFER");
                    qr.setQuantity(failQty);
                    qr.setRemainingQuantity(failQty);
                    qr.setReason(item.getQcFailureReason() != null ? item.getQcFailureReason() : "Hàng điều chuyển hỏng vật lý");
                    qr.setCreatedBy(actor);
                    qr.setCreatedAt(OffsetDateTime.now());
                    quarantineRecordRepository.save(qr);
                }
                BigDecimal shortageQty = qty.subtract(passQty).subtract(failQty);
                if (shortageQty.signum() > 0) {
                    Adjustment adjustment = Adjustment.builder()
                            .adjustmentNumber(generateAdjustmentNumber())
                            .warehouse(targetWarehouse)
                            .product(item.getProduct())
                            .batch(transit.getBatch())
                            .location(item.getDestinationLocation() != null ? item.getDestinationLocation() : helper.findQuarantineLocation(transfer))
                            .quantityAdjustment(shortageQty.negate())
                            .type(AdjustmentType.TRANSFER_DISCREPANCY)
                            .referenceId(transfer.getId())
                            .referenceType("TRANSFER")
                            .reason(request.discrepancyReason())
                            .documentDate(transfer.getDocumentDate())
                            .accountingPeriod(transfer.getAccountingPeriod())
                            .createdBy(actor)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    adjustmentRepository.save(adjustment);

                    auditUtil.logChange(actor, AuditAction.TRANSFER_DISCREPANCY_CREATE, "ADJUSTMENT",
                            adjustment.getId(), adjustment.getAdjustmentNumber(), Map.of(), Map.of());
                }
            }
        }
    }

    private void moveTransitToQuarantine(InterWarehouseTransfer transfer, User actor) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(InterWarehouseTransferHelper.IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation quarantineLocation = helper.findQuarantineLocation(transfer);
        Warehouse targetWarehouse = transfer.isReturned() ? transfer.getSourceWarehouse() : transfer.getDestinationWarehouse();

        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemId(item.getId())) {
                Inventory transit = inventoryRepository.findByStockKeyForUpdate(transitWarehouse.getId(),
                                item.getProduct().getId(), allocation.getInventory().getBatch().getId(),
                                helper.firstTransitLocation(transitWarehouse).getId())
                        .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_STOCK_NOT_FOUND"));
                BigDecimal qty = allocation.getAllocatedQty();
                transit.setTotalQty(transit.getTotalQty().subtract(qty));
                transit.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(transit);

                helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                        quarantineLocation, qty, transit.getCostPrice());

                // Lưu bản ghi vào quarantine_records khi từ chối nhận toàn bộ (bị chuyển vào quarantine)
                QuarantineRecord qr = new QuarantineRecord();
                qr.setWarehouse(targetWarehouse);
                qr.setProduct(item.getProduct());
                qr.setBatch(transit.getBatch());
                qr.setLocation(quarantineLocation);
                qr.setTransfer(transfer);
                qr.setTransferItem(item);
                qr.setOriginType("INTERNAL_TRANSFER");
                qr.setQuantity(qty);
                qr.setRemainingQuantity(qty);
                qr.setReason(transfer.getRejectionReason() != null ? transfer.getRejectionReason() : "Từ chối và cách ly toàn bộ hàng điều chuyển");
                qr.setCreatedBy(actor);
                qr.setCreatedAt(OffsetDateTime.now());
                quarantineRecordRepository.save(qr);
            }
        }
    }

    @Transactional
    public InterWarehouseTransferResponse requestReturn(Long id, TransferReturnRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnRequested(true);
        transfer.setReturnReason(request.reason());
        transfer.setReturnRequestedBy(actor);
        transfer.setReturnRequestedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse approveReturn(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (!transfer.isReturnRequested()) {
            throw new BusinessRuleViolationException("NO_RETURN_REQUESTED");
        }
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnApprovedBy(actor);
        transfer.setReturnApprovedAt(OffsetDateTime.now());
        transfer.setReturned(true);
        transfer.setReturnRequested(false);
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse rejectReturn(Long id, TransferReturnRejectRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (!transfer.isReturnRequested()) {
            throw new BusinessRuleViolationException("NO_RETURN_REQUESTED");
        }
        helper.ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnRejectedBy(actor);
        transfer.setReturnRejectedAt(OffsetDateTime.now());
        transfer.setReturnRejectionReason(request.reason());
        transfer.setReturnRequested(false);
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
