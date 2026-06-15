package com.wms.service;

import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.request.ReceiptReturnConfirmRequest;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.*;
import com.wms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for handling receipt approval, rejection, and putaway operations (US-WMS-05).
 */
@Service
public class ReceiptApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptApprovalService.class);

    private static final String RECEIPT_ENTITY = "RECEIPT";
    private static final String INVENTORY_ENTITY = "INVENTORY";

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;

    public ReceiptApprovalService(ReceiptRepository receiptRepository,
                                  ReceiptItemRepository receiptItemRepository,
                                  BatchRepository batchRepository,
                                  InventoryRepository inventoryRepository,
                                  WarehouseLocationRepository warehouseLocationRepository,
                                  ReceiptValidationService receiptValidationService,
                                  AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Approves a QC_COMPLETED receipt.
     */
    @Transactional
    public ReceiptActionResponse approveReceipt(Long receiptId,
                                                ReceiptDecisionRequest request,
                                                User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RECEIPT_APPROVE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());
        assertStatusForApproveOrReject(receipt);

        ReceiptStatus oldStatus = receipt.getStatus();

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        for (ReceiptItem item : items) {
            Batch batch = resolveOrCreateBatch(item, receipt);
            item.setBatch(batch);
            receiptItemRepository.save(item);
        }

        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_APPROVE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.APPROVED.name(),
                       "approvedBy", actor.getId(),
                       "approvedAt", receipt.getApprovedAt().toString())
        );

        log.info("Receipt {} approved by user {}", receiptId, actor.getId());
        return buildReceiptActionResponse(receipt, "Receipt approved successfully");
    }

    /**
     * Rejects a QC_COMPLETED receipt.
     */
    @Transactional
    public ReceiptActionResponse rejectReceipt(Long receiptId,
                                               ReceiptDecisionRequest request,
                                               User actor) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("REASON_REQUIRED: Rejection reason is mandatory");
        }

        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RECEIPT_REJECT");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());
        assertStatusForApproveOrReject(receipt);

        ReceiptStatus oldStatus = receipt.getStatus();

        receipt.setStatus(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING);
        receipt.setRejectionReason(request.getReason());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_REJECT, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.RETURN_TO_SUPPLIER_PENDING.name(),
                       "rejectionReason", request.getReason())
        );

        log.info("Receipt {} rejected by user {}", receiptId, actor.getId());
        return buildReceiptActionResponse(receipt, "Receipt rejected. Awaiting supplier vehicle handover.");
    }

    /**
     * Storekeeper confirms physical handover of rejected goods to supplier.
     */
    @Transactional
    public ReceiptActionResponse confirmReturnToSupplier(Long receiptId,
                                                          ReceiptReturnConfirmRequest request,
                                                          User actor) {
        receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "RECEIPT_RETURN_CONFIRM");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        if (receipt.getStatus() != ReceiptStatus.RETURN_TO_SUPPLIER_PENDING) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: Receipt " + receiptId + " must be RETURN_TO_SUPPLIER_PENDING "
                    + "to confirm supplier handover. Current status: " + receipt.getStatus());
        }

        ReceiptStatus oldStatus = receipt.getStatus();

        receipt.setStatus(ReceiptStatus.RETURNED_TO_SUPPLIER);
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_RETURN_CONFIRM, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.RETURNED_TO_SUPPLIER.name(),
                       "confirmedBy", actor.getId(),
                       "handoverNote", request.getHandoverNote() != null ? request.getHandoverNote() : "")
        );

        log.info("Receipt {} supplier handover confirmed by user {}", receiptId, actor.getId());
        return buildReceiptActionResponse(receipt, "Supplier handover confirmed. Receipt closed.");
    }

    /**
     * Storekeeper completes putaway of APPROVED receipt into a regular Bin.
     */
    @Transactional
    public ReceiptActionResponse completePutaway(Long receiptId,
                                                  ReceiptPutawayRequest request,
                                                  User actor) {
        receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "RECEIPT_PUTAWAY_COMPLETE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        if (receipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: Receipt " + receiptId + " must be APPROVED before putaway. "
                    + "Current status: " + receipt.getStatus());
        }

        WarehouseLocation location = warehouseLocationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Location not found: " + request.getLocationId()));

        if (Boolean.TRUE.equals(location.getIsQuarantine())) {
            throw new BusinessRuleViolationException(
                    "INVALID_LOCATION: Putaway target must be a regular (non-quarantine) Bin. "
                    + "Location " + request.getLocationId() + " is a quarantine location.");
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        assertBinCapacity(location, items);
        for (ReceiptItem item : items) {
            if (item.getBatch() == null) {
                throw new BusinessRuleViolationException(
                        "MISSING_BATCH: Receipt item " + item.getId() + " has no batch assigned. "
                        + "Ensure receipt is properly APPROVED with batch resolution.");
            }
            increaseRegularInventory(receipt, item, location, actor);
            item.setLocation(location);
            receiptItemRepository.save(item);
        }
        applyBinOccupancy(location, items);

        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.RECEIPT_PUTAWAY_COMPLETE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(),
                receipt.getWarehouse().getId(),
                Map.of("status", ReceiptStatus.APPROVED.name()),
                Map.of("locationId", request.getLocationId(), "putawayCompletedBy", actor.getId())
        );

        log.info("Receipt {} putaway completed by user {} at location {}",
                receiptId, actor.getId(), request.getLocationId());
        return buildReceiptActionResponse(receipt, "Putaway completed. Regular inventory updated.");
    }



    private void assertStatusForApproveOrReject(Receipt receipt) {
        ReceiptStatus status = receipt.getStatus();
        if (status == ReceiptStatus.APPROVED
                || status == ReceiptStatus.RETURN_TO_SUPPLIER_PENDING
                || status == ReceiptStatus.RETURNED_TO_SUPPLIER) {
            throw new ReceiptAlreadyDecidedException(receipt.getId(), status);
        }
        if (status != ReceiptStatus.QC_COMPLETED) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: Approve/reject requires QC_COMPLETED status. "
                    + "Receipt " + receipt.getId() + " has status: " + status);
        }
    }



    private Batch resolveOrCreateBatch(ReceiptItem item, Receipt receipt) {
        Long productId = item.getProduct().getId();
        Long warehouseId = receipt.getWarehouse().getId();
        LocalDate receivedDate = receipt.getDocumentDate();

        return batchRepository
                .findByProductWarehouseAndReceivedDate(productId, warehouseId, receivedDate)
                .orElseGet(() -> {
                    String batchNumber = String.format("BCH-%d-%s-%s",
                            productId,
                            receipt.getReceiptNumber(),
                            receivedDate.toString());
                    Batch newBatch = Batch.builder()
                            .batchNumber(batchNumber)
                            .product(item.getProduct())
                            .warehouse(receipt.getWarehouse())
                            .receivedDate(receivedDate)
                            .quantity(item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO)
                            .createdAt(OffsetDateTime.now())
                            .build();
                    return batchRepository.save(newBatch);
                });
    }

    private void increaseRegularInventory(Receipt receipt, ReceiptItem item,
                                           WarehouseLocation location, User actor) {
        BigDecimal qty = item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO;
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long warehouseId = receipt.getWarehouse().getId();
        Long productId = item.getProduct().getId();
        Long batchId = item.getBatch().getId();
        Long locationId = location.getId();

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouseId, productId, batchId, locationId)
                .orElseGet(() -> Inventory.builder()
                        .warehouse(receipt.getWarehouse())
                        .product(item.getProduct())
                        .batch(item.getBatch())
                        .location(location)
                        .totalQty(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .costPrice(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                        .updatedAt(OffsetDateTime.now())
                        .build());

        BigDecimal oldQty = inventory.getTotalQty();
        inventory.setTotalQty(oldQty.add(qty));
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);

        auditLogService.log(
                actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(),
                "INV-" + warehouseId + "-" + productId,
                warehouseId,
                Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", inventory.getTotalQty(), "reservedQty", inventory.getReservedQty(),
                       "locationId", locationId, "delta", qty)
        );
    }

    private void assertBinCapacity(WarehouseLocation location, List<ReceiptItem> items) {
        BigDecimal addedVolume = calculateAddedVolume(items);
        BigDecimal addedWeight = calculateAddedWeight(items);
        BigDecimal currentVolume = zeroIfNull(location.getCurrentVolumeM3());
        BigDecimal currentWeight = zeroIfNull(location.getCurrentWeightKg());

        if (location.getCapacityM3() != null
                && currentVolume.add(addedVolume).compareTo(location.getCapacityM3()) > 0) {
            throw new BusinessRuleViolationException(
                    "BIN_CAPACITY_EXCEEDED: Putaway volume exceeds location capacity for location "
                    + location.getId());
        }
        if (location.getCapacityKg() != null
                && currentWeight.add(addedWeight).compareTo(location.getCapacityKg()) > 0) {
            throw new BusinessRuleViolationException(
                    "BIN_CAPACITY_EXCEEDED: Putaway weight exceeds location capacity for location "
                    + location.getId());
        }
    }

    private void applyBinOccupancy(WarehouseLocation location, List<ReceiptItem> items) {
        location.setCurrentVolumeM3(zeroIfNull(location.getCurrentVolumeM3()).add(calculateAddedVolume(items)));
        location.setCurrentWeightKg(zeroIfNull(location.getCurrentWeightKg()).add(calculateAddedWeight(items)));
        location.setUpdatedAt(OffsetDateTime.now());
        warehouseLocationRepository.save(location);
    }

    private BigDecimal calculateAddedVolume(List<ReceiptItem> items) {
        return items.stream()
                .map(item -> getActualQtyAsBigDecimal(item)
                        .multiply(zeroIfNull(item.getProduct().getVolumeM3())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAddedWeight(List<ReceiptItem> items) {
        return items.stream()
                .map(item -> getActualQtyAsBigDecimal(item)
                        .multiply(zeroIfNull(item.getProduct().getWeightKg())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getActualQtyAsBigDecimal(ReceiptItem item) {
        return item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private ReceiptActionResponse buildReceiptActionResponse(Receipt receipt, String message) {
        return ReceiptActionResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .version(receipt.getVersion())
                .updatedAt(receipt.getUpdatedAt())
                .message(message)
                .build();
    }
}
