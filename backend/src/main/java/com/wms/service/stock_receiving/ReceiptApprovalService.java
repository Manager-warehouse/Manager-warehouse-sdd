package com.wms.service.stock_receiving;

import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptItemUnitCostRequest;
import com.wms.dto.request.ReceiptPutawayItem;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.request.ReceiptReturnConfirmRequest;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ReceiptAlreadyDecidedException;
import com.wms.exception.ReceiptCountException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.BatchRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.SupplierBillingNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReceiptApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptApprovalService.class);
    private static final String RECEIPT_ENTITY = "RECEIPT";
    private static final String INVENTORY_ENTITY = "INVENTORY";
    private static final DateTimeFormatter BATCH_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;
    private final SupplierBillingNotificationService supplierBillingNotificationService;

    public ReceiptApprovalService(ReceiptRepository receiptRepository,
                                  ReceiptItemRepository receiptItemRepository,
                                  BatchRepository batchRepository,
                                  InventoryRepository inventoryRepository,
                                  WarehouseLocationRepository warehouseLocationRepository,
                                  ReceiptValidationService receiptValidationService,
                                  AuditLogService auditLogService,
                                  SupplierBillingNotificationService supplierBillingNotificationService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.batchRepository = batchRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
        this.supplierBillingNotificationService = supplierBillingNotificationService;
    }

    @Transactional
    public ReceiptActionResponse approveReceipt(Long receiptId, ReceiptDecisionRequest request, User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RECEIPT_APPROVE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());
        assertStatusForApprove(receipt);

        ReceiptStatus oldStatus = receipt.getStatus();
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        Map<Long, BigDecimal> unitCosts = unitCostByItemId(request);
        int totalApprovedQty = 0;

        for (ReceiptItem item : items) {
            int approvedQty = approvedQtyFor(receipt, item);
            applyUnitCost(item, unitCosts.get(item.getId()), approvedQty);
            item.setApprovedQty(approvedQty);
            if (approvedQty > 0) {
                item.setBatch(resolveOrCreateBatch(item, receipt, approvedQty));
            }
            receiptItemRepository.save(item);
            totalApprovedQty += approvedQty;
        }

        if (receipt.getStatus() == ReceiptStatus.QC_FAILED && totalApprovedQty <= 0) {
            throw new BusinessRuleViolationException("NO_PASSED_QUANTITY_TO_APPROVE");
        }
        if (receipt.getStatus() == ReceiptStatus.QC_FAILED) {
            finalizeQuarantine(receipt, items, actor);
        }

        ReceiptStatus newStatus = oldStatus == ReceiptStatus.QC_FAILED
                ? ReceiptStatus.PARTIALLY_APPROVED
                : ReceiptStatus.APPROVED;
        receipt.setStatus(newStatus);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        AuditAction action = newStatus == ReceiptStatus.PARTIALLY_APPROVED
                ? AuditAction.RECEIPT_PARTIAL_APPROVE
                : AuditAction.RECEIPT_APPROVE;
        auditLogService.log(actor, action, RECEIPT_ENTITY, receipt.getId(),
                receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", newStatus.name(), "approvedBy", actor.getId(),
                        "approvedQty", totalApprovedQty));

        log.info("Receipt {} approved with status {} by user {}", receiptId, newStatus, actor.getId());
        return buildReceiptActionResponse(receipt, "Receipt approved successfully");
    }

    @Transactional
    public ReceiptActionResponse rejectReceipt(Long receiptId, ReceiptDecisionRequest request, User actor) {
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new IllegalArgumentException("REASON_REQUIRED: Rejection reason is mandatory");
        }

        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RECEIPT_REJECT");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());
        assertStatusForReject(receipt);

        ReceiptStatus oldStatus = receipt.getStatus();
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        if (oldStatus == ReceiptStatus.QC_FAILED) {
            finalizeQuarantine(receipt, items, actor);
        }
        for (ReceiptItem item : items) {
            item.setApprovedQty(0);
            receiptItemRepository.save(item);
        }

        receipt.setStatus(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING);
        receipt.setRejectionReason(request.getReason());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_REJECT, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.RETURN_TO_SUPPLIER_PENDING.name(),
                        "rejectionReason", request.getReason()));

        return buildReceiptActionResponse(receipt, "Receipt rejected. Awaiting supplier handover.");
    }

    @Transactional
    public ReceiptActionResponse confirmReturnToSupplier(Long receiptId,
                                                          ReceiptReturnConfirmRequest request,
                                                          User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "RECEIPT_RETURN_CONFIRM");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());
        if (receipt.getStatus() != ReceiptStatus.RETURN_TO_SUPPLIER_PENDING) {
            throw new BusinessRuleViolationException("INVALID_STATE: Receipt must be RETURN_TO_SUPPLIER_PENDING");
        }

        ReceiptStatus oldStatus = receipt.getStatus();
        receipt.setStatus(ReceiptStatus.RETURNED_TO_SUPPLIER);
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(actor, AuditAction.RECEIPT_RETURN_CONFIRM, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.RETURNED_TO_SUPPLIER.name(),
                        "confirmedBy", actor.getId(),
                        "handoverNote", request.getHandoverNote() != null ? request.getHandoverNote() : ""));
        return buildReceiptActionResponse(receipt, "Supplier handover confirmed. Receipt closed.");
    }

    @Transactional
    public ReceiptActionResponse completePutaway(Long receiptId, ReceiptPutawayRequest request, User actor) {
        receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "RECEIPT_PUTAWAY_COMPLETE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        if (receipt.getStatus() == ReceiptStatus.PUTAWAY_COMPLETED || receipt.getPutawayCompletedAt() != null) {
            throw new ReceiptCountException("PUTAWAY_ALREADY_COMPLETED", HttpStatus.CONFLICT,
                    "Putaway has already been completed for receipt " + receiptId);
        }
        if (receipt.getStatus() != ReceiptStatus.APPROVED
                && receipt.getStatus() != ReceiptStatus.PARTIALLY_APPROVED) {
            throw new BusinessRuleViolationException("INVALID_STATE: Receipt must be APPROVED or PARTIALLY_APPROVED");
        }

        List<ReceiptItem> receiptItems = receiptItemRepository.findByReceiptId(receiptId);
        Map<Long, ReceiptPutawayItem> allocations = allocationByItemId(request);
        Map<WarehouseLocation, List<ReceiptItem>> itemsByLocation = new HashMap<>();
        for (ReceiptItem item : receiptItems) {
            int approvedQty = safe(item.getApprovedQty());
            if (approvedQty <= 0) {
                continue;
            }
            ReceiptPutawayItem allocation = allocations.get(item.getId());
            if (allocation == null) {
                throw new BusinessRuleViolationException("LOCATION_REQUIRED: item " + item.getId());
            }
            if (!Integer.valueOf(approvedQty).equals(allocation.getQuantity())) {
                throw new BusinessRuleViolationException("PUTAWAY_QTY_MISMATCH: item " + item.getId());
            }
            WarehouseLocation location = loadRegularBin(allocation.getLocationId(), receipt);
            itemsByLocation.computeIfAbsent(location, key -> new java.util.ArrayList<>()).add(item);
        }

        for (Map.Entry<WarehouseLocation, List<ReceiptItem>> entry : itemsByLocation.entrySet()) {
            assertBinCapacity(entry.getKey(), entry.getValue());
            for (ReceiptItem item : entry.getValue()) {
                if (item.getBatch() == null) {
                    throw new BusinessRuleViolationException("MISSING_BATCH: item " + item.getId());
                }
                assertExpectedBatchCode(allocations.get(item.getId()), item);
                increaseRegularInventory(receipt, item, entry.getKey(), actor);
                item.setLocation(entry.getKey());
                receiptItemRepository.save(item);
            }
            applyBinOccupancy(entry.getKey(), entry.getValue());
        }

        ReceiptStatus oldStatus = receipt.getStatus();
        receipt.setStatus(ReceiptStatus.PUTAWAY_COMPLETED);
        receipt.setPutawayCompletedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        supplierBillingNotificationService.createNotificationForReceiptOrder(receipt);
        auditLogService.log(actor, AuditAction.RECEIPT_PUTAWAY_COMPLETE, RECEIPT_ENTITY,
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                Map.of("status", oldStatus.name()),
                Map.of("status", ReceiptStatus.PUTAWAY_COMPLETED.name(),
                        "putawayCompletedBy", actor.getId(), "itemsCount", request.getItems().size(),
                        "batchCodes", collectBatchCodes(receipt.getId())));
        return buildReceiptActionResponse(receipt, "Putaway completed. Regular inventory updated.");
    }

    private void assertStatusForApprove(Receipt receipt) {
        if (receipt.getStatus() == ReceiptStatus.APPROVED
                || receipt.getStatus() == ReceiptStatus.PARTIALLY_APPROVED
                || receipt.getStatus() == ReceiptStatus.RETURN_TO_SUPPLIER_PENDING
                || receipt.getStatus() == ReceiptStatus.RETURNED_TO_SUPPLIER) {
            throw new ReceiptAlreadyDecidedException(receipt.getId(), receipt.getStatus());
        }
        if (receipt.getStatus() == ReceiptStatus.PENDING_STOREKEEPER_REVIEW
                || receipt.getStatus() == ReceiptStatus.RECOUNT_REQUIRED) {
            throw new BusinessRuleViolationException("STOREKEEPER_REVIEW_PENDING");
        }
        if (receipt.getStatus() != ReceiptStatus.QC_COMPLETED && receipt.getStatus() != ReceiptStatus.QC_FAILED) {
            throw new BusinessRuleViolationException("INVALID_STATE: Approve requires QC_COMPLETED or QC_FAILED");
        }
    }

    private void assertStatusForReject(Receipt receipt) {
        if (receipt.getStatus() == ReceiptStatus.PENDING_STOREKEEPER_REVIEW
                || receipt.getStatus() == ReceiptStatus.RECOUNT_REQUIRED) {
            throw new BusinessRuleViolationException("STOREKEEPER_REVIEW_PENDING");
        }
        if (receipt.getStatus() != ReceiptStatus.QC_COMPLETED && receipt.getStatus() != ReceiptStatus.QC_FAILED) {
            throw new BusinessRuleViolationException("INVALID_STATE: Reject requires QC_COMPLETED or QC_FAILED");
        }
    }

    private int approvedQtyFor(Receipt receipt, ReceiptItem item) {
        return receipt.getStatus() == ReceiptStatus.QC_FAILED
                ? safe(item.getQualityPassedQty())
                : safe(item.getActualQty());
    }

    private void applyUnitCost(ReceiptItem item, BigDecimal requestUnitCost, int approvedQty) {
        if (requestUnitCost != null) {
            item.setUnitCost(requestUnitCost);
        }
        if (approvedQty > 0 && (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessRuleViolationException("UNIT_COST_REQUIRED: item " + item.getId());
        }
    }

    private Map<Long, BigDecimal> unitCostByItemId(ReceiptDecisionRequest request) {
        if (request.getItemUnitCosts() == null) {
            return Map.of();
        }
        return request.getItemUnitCosts().stream()
                .collect(Collectors.toMap(
                        ReceiptItemUnitCostRequest::getReceiptItemId,
                        ReceiptItemUnitCostRequest::getUnitCost,
                        (left, right) -> right));
    }

    private Map<Long, ReceiptPutawayItem> allocationByItemId(ReceiptPutawayRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("ITEMS_REQUIRED");
        }
        return request.getItems().stream()
                .collect(Collectors.toMap(ReceiptPutawayItem::getReceiptItemId, item -> item, (left, right) -> right));
    }

    private WarehouseLocation loadRegularBin(Long locationId, Receipt receipt) {
        WarehouseLocation location = warehouseLocationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
        if (!Boolean.TRUE.equals(location.getIsActive())
                || Boolean.TRUE.equals(location.getIsQuarantine())
                || !location.getWarehouse().getId().equals(receipt.getWarehouse().getId())) {
            throw new BusinessRuleViolationException("PUTAWAY_LOCATION_INVALID: " + locationId);
        }
        return location;
    }

    private Batch resolveOrCreateBatch(ReceiptItem item, Receipt receipt, int quantity) {
        LocalDate receivedDate = receipt.getDocumentDate();
        String batchCode = buildBatchCode(receipt, item);

        return batchRepository.findByBatchCode(batchCode)
                .orElseGet(() -> batchRepository.save(Batch.builder()
                        .batchNumber(batchCode)
                        .batchCode(batchCode)
                        .product(item.getProduct())
                        .warehouse(receipt.getWarehouse())
                        .receivedDate(receivedDate)
                        .quantity(BigDecimal.valueOf(quantity))
                        .createdAt(OffsetDateTime.now())
                        .build()));
    }

    private String buildBatchCode(Receipt receipt, ReceiptItem item) {
        String warehouseCode = receipt.getWarehouse().getCode();
        if (warehouseCode == null || warehouseCode.isBlank()) {
            warehouseCode = "W" + receipt.getWarehouse().getId();
        }
        String normalizedWarehouseCode = warehouseCode.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "");
        LocalDate receivedDate = receipt.getDocumentDate();
        long sequenceSeed = item.getId() != null ? item.getId() : receipt.getId();
        return "LOT-" + normalizedWarehouseCode + "-" + receivedDate.format(BATCH_DATE)
                + "-" + String.format("%04d", sequenceSeed);
    }

    private void assertExpectedBatchCode(ReceiptPutawayItem allocation, ReceiptItem item) {
        String expectedBatchCode = allocation != null ? allocation.getExpectedBatchCode() : null;
        if (expectedBatchCode == null || expectedBatchCode.isBlank()) {
            return;
        }
        String actualBatchCode = readableBatchCode(item.getBatch());
        if (!expectedBatchCode.equals(actualBatchCode)) {
            throw new BusinessRuleViolationException("BATCH_CODE_MISMATCH: item " + item.getId());
        }
    }

    private void finalizeQuarantine(Receipt receipt, List<ReceiptItem> items, User actor) {
        if (items.stream().noneMatch(item -> safe(item.getQuarantineReadyQty()) > 0)) {
            return;
        }
        WarehouseLocation quarantineLoc = warehouseLocationRepository
                .findFirstByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(receipt.getWarehouse().getId())
                .orElseThrow(() -> new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED"));
        assertBinCapacity(quarantineLoc, items);
        applyBinOccupancy(quarantineLoc, items);
        for (ReceiptItem item : items) {
            int readyQty = safe(item.getQuarantineReadyQty());
            if (readyQty <= 0) {
                continue;
            }
            if (item.getBatch() == null) {
                item.setBatch(resolveOrCreateBatch(item, receipt, Math.max(safe(item.getApprovedQty()), readyQty)));
            }
            increaseQuarantineInventory(receipt, item, quarantineLoc, readyQty, actor);
            item.setQuarantineQty(safe(item.getQuarantineQty()) + readyQty);
            item.setQuarantineReadyQty(0);
            item.setLocation(quarantineLoc);
            receiptItemRepository.save(item);
        }
    }

    private void increaseRegularInventory(Receipt receipt, ReceiptItem item, WarehouseLocation location, User actor) {
        increaseInventory(receipt, item, location, BigDecimal.valueOf(safe(item.getApprovedQty())), actor, "PUTAWAY");
    }

    private void increaseQuarantineInventory(Receipt receipt, ReceiptItem item, WarehouseLocation location,
                                             int quantity, User actor) {
        increaseInventory(receipt, item, location, BigDecimal.valueOf(quantity), actor, "QUARANTINE_FINALIZE");
    }

    private void increaseInventory(Receipt receipt, ReceiptItem item, WarehouseLocation location,
                                   BigDecimal qty, User actor, String reason) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long warehouseId = receipt.getWarehouse().getId();
        Long productId = item.getProduct().getId();
        Long batchId = item.getBatch().getId();
        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouseId, productId, batchId, location.getId())
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

        auditLogService.log(actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(), "INV-" + warehouseId + "-" + productId,
                warehouseId, Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", inventory.getTotalQty(), "reservedQty", inventory.getReservedQty(),
                        "locationId", location.getId(), "delta", qty, "reason", reason,
                        "batchId", batchId, "batchCode", readableBatchCode(item.getBatch())));
    }

    private void assertBinCapacity(WarehouseLocation location, List<ReceiptItem> items) {
        BigDecimal addedVolume = calculateAddedVolume(items);
        BigDecimal addedWeight = calculateAddedWeight(items);
        if (location.getCapacityM3() != null
                && zeroIfNull(location.getCurrentVolumeM3()).add(addedVolume).compareTo(location.getCapacityM3()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: location " + location.getId());
        }
        if (location.getCapacityKg() != null
                && zeroIfNull(location.getCurrentWeightKg()).add(addedWeight).compareTo(location.getCapacityKg()) > 0) {
            throw new BusinessRuleViolationException("BIN_CAPACITY_EXCEEDED: location " + location.getId());
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
                .map(item -> BigDecimal.valueOf(quantityForOccupancy(item))
                        .multiply(zeroIfNull(item.getProduct().getVolumeM3())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAddedWeight(List<ReceiptItem> items) {
        return items.stream()
                .map(item -> BigDecimal.valueOf(quantityForOccupancy(item))
                        .multiply(zeroIfNull(item.getProduct().getWeightKg())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int quantityForOccupancy(ReceiptItem item) {
        return safe(item.getQuarantineReadyQty()) > 0 ? safe(item.getQuarantineReadyQty()) : safe(item.getApprovedQty());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }

    private ReceiptActionResponse buildReceiptActionResponse(Receipt receipt, String message) {
        return ReceiptActionResponse.builder()
                .id(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .version(receipt.getVersion())
                .updatedAt(receipt.getUpdatedAt())
                .message(message)
                .batchCodes(collectBatchCodes(receipt.getId()))
                .build();
    }

    private List<String> collectBatchCodes(Long receiptId) {
        Set<String> batchCodes = receiptItemRepository.findByReceiptId(receiptId).stream()
                .map(ReceiptItem::getBatch)
                .filter(batch -> batch != null)
                .map(this::readableBatchCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return List.copyOf(batchCodes);
    }

    private String readableBatchCode(Batch batch) {
        return batch.getBatchCode() != null ? batch.getBatchCode() : batch.getBatchNumber();
    }
}
