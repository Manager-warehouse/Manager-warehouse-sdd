package com.wms.service.warehouse_transfer.impl;
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

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.util.PartnerAuditUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    private final DiscrepancyIncidentRepository discrepancyIncidentRepository;
    private final DiscrepancyHoldEntryRepository discrepancyHoldEntryRepository;
    private final ProductRepository productRepository;
    private final WrongSkuReportRepository wrongSkuReportRepository;
    private final WrongSkuReportItemRepository wrongSkuReportItemRepository;

    @Transactional
    public InterWarehouseTransferResponse receiveCount(Long id, InterWarehouseTransferReceiveCountRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        helper.ensureWarehouseScope(actor, transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId());

        if (Boolean.TRUE.equals(transfer.isReturned())) {
            if (transfer.getReturnArrivedAt() == null) {
                throw new BusinessRuleViolationException("RETURN_ARRIVE_REQUIRED");
            }
            if (transfer.getReturnArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("RETURN_HANDOVER_REQUIRED");
            }
        } else {
            if (transfer.getDriverArrivedAt() == null) {
                throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
            }
            if (transfer.getArrivalHandoverAt() == null) {
                throw new BusinessRuleViolationException("ARRIVAL_HANDOVER_REQUIRED");
            }
        }

        Map<Long, InterWarehouseTransferItem> itemById = helper.itemMap(transfer);
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferReceiveCountItemRequest line : request.items()) {
            InterWarehouseTransferItem item = helper.requireItem(itemById, line.transferItemId());
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
        if (helper.isBlank(request.qcPhotoRef())) {
            throw new BusinessRuleViolationException("RECEIVE_QC_PHOTO_REQUIRED");
        }
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
        transfer.setReceiveQcPhotoRef(request.qcPhotoRef());
        transfer.setUpdatedAt(OffsetDateTime.now());
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
    public InterWarehouseTransferResponse returnToSource(Long id, TransferReturnRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            if (actor.getRole() != UserRole.WAREHOUSE_MANAGER) {
                throw new BusinessRuleViolationException("WAREHOUSE_MANAGER_ROLE_REQUIRED");
            }
            ensureManagerCanRequestReturn(transfer, actor);
        }

        if (helper.isBlank(request.reason())) {
            throw new BusinessRuleViolationException("RETURN_REASON_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturned(true);
        transfer.setReturnReason(request.reason());
        transfer.setUpdatedAt(OffsetDateTime.now());

        if (request.wrongSkuItems() != null && !request.wrongSkuItems().isEmpty()) {
            WrongSkuReport report = WrongSkuReport.builder()
                    .transfer(transfer)
                    .status("APPROVED") // Auto-approved because it's initiated by manager/planner
                    .reportedBy(actor)
                    .reportedAt(OffsetDateTime.now())
                    .managerDecisionBy(actor)
                    .managerDecisionAt(OffsetDateTime.now())
                    .build();
            report = wrongSkuReportRepository.save(report);

            for (WrongSkuItemRequest line : request.wrongSkuItems()) {
                InterWarehouseTransferItem item = transferItemRepository.findById(line.transferItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Transfer item not found: " + line.transferItemId()));
                Product expected = productRepository.findById(line.expectedProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.expectedProductId()));
                Product actual = productRepository.findById(line.actualProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.actualProductId()));

                WrongSkuReportItem reportItem = WrongSkuReportItem.builder()
                        .report(report)
                        .transferItem(item)
                        .expectedProduct(expected)
                        .actualProduct(actual)
                        .affectedQty(line.affectedQty())
                        .reason(line.reason())
                        .photoRef(line.photoRef())
                        .build();
                wrongSkuReportItemRepository.save(reportItem);
            }
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_TO_SOURCE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void ensureManagerCanRequestReturn(InterWarehouseTransfer transfer, User actor) {
        List<Long> warehouseIds = helper.loadWarehouseIds(actor);
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long destinationWarehouseId = transfer.getDestinationWarehouse().getId();
        if (!warehouseIds.contains(sourceWarehouseId) && !warehouseIds.contains(destinationWarehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
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

        // T049: Validate bin capacity before inventory posting (dry-run check first)
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal passedQty = helper.zero(item.getQcPassedQty());
            BigDecimal failedQty = helper.zero(item.getQcFailedQty());
            if (passedQty.signum() > 0) {
                assertLocationCapacity(item.getDestinationLocation(), item.getProduct(), passedQty);
            }
            if (failedQty.signum() > 0) {
                if (quarantineLocation == null) {
                    quarantineLocation = helper.findQuarantineLocation(transfer);
                }
                assertLocationCapacity(quarantineLocation, item.getProduct(), failedQty);
            }
        }

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
                    applyLocationOccupancy(item.getDestinationLocation(), item.getProduct(), passQty);
                    helper.upsertInventory(targetWarehouse, item.getProduct(), transit.getBatch(),
                            item.getDestinationLocation(), passQty, transit.getCostPrice());
                    remainingPassed = remainingPassed.subtract(passQty);
                }
                BigDecimal failQty = qty.subtract(passQty).min(remainingFailed);
                if (failQty.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = helper.findQuarantineLocation(transfer);
                    }
                    applyLocationOccupancy(quarantineLocation, item.getProduct(), failQty);
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

            // T050: Add discrepancy hold for over-receipt (excess quantity not satisfied by transit allocation)
            BigDecimal overReceiptPassed = remainingPassed;
            BigDecimal overReceiptFailed = remainingFailed;
            BigDecimal totalOverReceipt = overReceiptPassed.add(overReceiptFailed);
            if (totalOverReceipt.signum() > 0) {
                DiscrepancyIncident incident = DiscrepancyIncident.builder()
                        .transfer(transfer)
                        .product(item.getProduct())
                        .incidentType("OVER_RECEIPT")
                        .quantity(totalOverReceipt)
                        .status("OPEN")
                        .resolutionNote("Over-receipt during transfer receiving")
                        .build();
                incident = discrepancyIncidentRepository.save(incident);

                Batch batch = item.getBatch();
                if (batch == null && !allocationRepository.findByTransferItemId(item.getId()).isEmpty()) {
                    batch = allocationRepository.findByTransferItemId(item.getId()).get(0).getInventory().getBatch();
                }

                BigDecimal costPrice = BigDecimal.ZERO;
                if (!allocationRepository.findByTransferItemId(item.getId()).isEmpty()) {
                    costPrice = allocationRepository.findByTransferItemId(item.getId()).get(0).getInventory().getCostPrice();
                }

                if (overReceiptPassed.signum() > 0) {
                    applyLocationOccupancy(item.getDestinationLocation(), item.getProduct(), overReceiptPassed);
                    helper.upsertInventory(targetWarehouse, item.getProduct(), batch,
                            item.getDestinationLocation(), overReceiptPassed, costPrice);
                    discrepancyHoldEntryRepository.save(DiscrepancyHoldEntry.builder()
                            .incident(incident)
                            .warehouse(targetWarehouse)
                            .product(item.getProduct())
                            .batch(batch)
                            .holdQty(overReceiptPassed)
                            .holdLocation(item.getDestinationLocation())
                            .build());
                }
                if (overReceiptFailed.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = helper.findQuarantineLocation(transfer);
                    }
                    applyLocationOccupancy(quarantineLocation, item.getProduct(), overReceiptFailed);
                    helper.upsertInventory(targetWarehouse, item.getProduct(), batch,
                            quarantineLocation, overReceiptFailed, costPrice);
                    discrepancyHoldEntryRepository.save(DiscrepancyHoldEntry.builder()
                            .incident(incident)
                            .warehouse(targetWarehouse)
                            .product(item.getProduct())
                            .batch(batch)
                            .holdQty(overReceiptFailed)
                            .holdLocation(quarantineLocation)
                            .build());

                    // Save QuarantineRecord for over-received failed QC
                    QuarantineRecord qr = new QuarantineRecord();
                    qr.setWarehouse(targetWarehouse);
                    qr.setProduct(item.getProduct());
                    qr.setBatch(batch);
                    qr.setLocation(quarantineLocation);
                    qr.setTransfer(transfer);
                    qr.setTransferItem(item);
                    qr.setOriginType("INTERNAL_TRANSFER");
                    qr.setQuantity(overReceiptFailed);
                    qr.setRemainingQuantity(overReceiptFailed);
                    qr.setReason(item.getQcFailureReason() != null ? item.getQcFailureReason() : "Over-receipt QC failed");
                    qr.setCreatedBy(actor);
                    qr.setCreatedAt(OffsetDateTime.now());
                    quarantineRecordRepository.save(qr);
                }
            }
        }
    }

    private void moveTransitToQuarantine(InterWarehouseTransfer transfer, User actor) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(InterWarehouseTransferHelper.IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation quarantineLocation = helper.findQuarantineLocation(transfer);
        Warehouse targetWarehouse = transfer.isReturned() ? transfer.getSourceWarehouse() : transfer.getDestinationWarehouse();

        // T049: Validate bin capacity for quarantine location (dry run check first)
        BigDecimal totalQtyToQuarantine = BigDecimal.ZERO;
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getSentQty() != null ? item.getSentQty() : item.getPlannedQty();
            totalQtyToQuarantine = totalQtyToQuarantine.add(qty);
            assertLocationCapacity(quarantineLocation, item.getProduct(), qty);
        }

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

                applyLocationOccupancy(quarantineLocation, item.getProduct(), qty);
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

        if (request.wrongSkuItems() != null && !request.wrongSkuItems().isEmpty()) {
            WrongSkuReport report = WrongSkuReport.builder()
                    .transfer(transfer)
                    .status("PENDING")
                    .reportedBy(actor)
                    .reportedAt(OffsetDateTime.now())
                    .build();
            report = wrongSkuReportRepository.save(report);

            for (WrongSkuItemRequest line : request.wrongSkuItems()) {
                InterWarehouseTransferItem item = transferItemRepository.findById(line.transferItemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Transfer item not found: " + line.transferItemId()));
                Product expected = productRepository.findById(line.expectedProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.expectedProductId()));
                Product actual = productRepository.findById(line.actualProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + line.actualProductId()));

                WrongSkuReportItem reportItem = WrongSkuReportItem.builder()
                        .report(report)
                        .transferItem(item)
                        .expectedProduct(expected)
                        .actualProduct(actual)
                        .affectedQty(line.affectedQty())
                        .reason(line.reason())
                        .photoRef(line.photoRef())
                        .build();
                wrongSkuReportItemRepository.save(reportItem);
            }
        }

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

        // Approve pending wrong SKU reports
        List<WrongSkuReport> pendingReports = wrongSkuReportRepository.findByTransferId(transfer.getId());
        for (WrongSkuReport report : pendingReports) {
            if ("PENDING".equals(report.getStatus())) {
                report.setStatus("APPROVED");
                report.setManagerDecisionBy(actor);
                report.setManagerDecisionAt(OffsetDateTime.now());
                wrongSkuReportRepository.save(report);
            }
        }

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

        // Reject pending wrong SKU reports
        List<WrongSkuReport> pendingReports = wrongSkuReportRepository.findByTransferId(transfer.getId());
        for (WrongSkuReport report : pendingReports) {
            if ("PENDING".equals(report.getStatus())) {
                report.setStatus("REJECTED");
                report.setManagerDecisionBy(actor);
                report.setManagerDecisionAt(OffsetDateTime.now());
                report.setManagerNote(request.reason());
                wrongSkuReportRepository.save(report);
            }
        }

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.UPDATE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void assertLocationCapacity(WarehouseLocation location, Product product, BigDecimal qty) {
        if (location == null || qty == null || qty.signum() <= 0) {
            return;
        }
        BigDecimal addedVolume = product.getVolumeM3() != null ? product.getVolumeM3().multiply(qty) : BigDecimal.ZERO;
        BigDecimal addedWeight = product.getWeightKg() != null ? product.getWeightKg().multiply(qty) : BigDecimal.ZERO;

        BigDecimal currentVolume = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal currentWeight = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;

        if (location.getCapacityM3() != null && currentVolume.add(addedVolume).compareTo(location.getCapacityM3()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Volume exceeds location capacity for " + location.getCode());
        }
        if (location.getCapacityKg() != null && currentWeight.add(addedWeight).compareTo(location.getCapacityKg()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: Weight exceeds location capacity for " + location.getCode());
        }
    }

    private void applyLocationOccupancy(WarehouseLocation location, Product product, BigDecimal qty) {
        if (location == null || qty == null || qty.signum() <= 0) {
            return;
        }
        BigDecimal addedVolume = product.getVolumeM3() != null ? product.getVolumeM3().multiply(qty) : BigDecimal.ZERO;
        BigDecimal addedWeight = product.getWeightKg() != null ? product.getWeightKg().multiply(qty) : BigDecimal.ZERO;

        BigDecimal currentVolume = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal currentWeight = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;

        location.setCurrentVolumeM3(currentVolume.add(addedVolume));
        location.setCurrentWeightKg(currentWeight.add(addedWeight));
        location.setUpdatedAt(OffsetDateTime.now());
        locationRepository.save(location);
    }
}
