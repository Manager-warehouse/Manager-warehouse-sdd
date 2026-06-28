package com.wms.service;

import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.dto.response.QuarantineItemResponse;
import com.wms.dto.response.RtvActionResponse;
import java.util.stream.Collectors;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling quarantine Return To Vendor (RTV) operations (US-WMS-04).
 */
@Service
public class QuarantineRtvService {

    private static final Logger log = LoggerFactory.getLogger(QuarantineRtvService.class);

    private static final String ADJUSTMENT_ENTITY = "ADJUSTMENT";
    private static final String INVENTORY_ENTITY = "INVENTORY";
    private static final String RTV_REFERENCE_TYPE = "RECEIPT";

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final InventoryRepository inventoryRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;

    public QuarantineRtvService(ReceiptRepository receiptRepository,
                                 ReceiptItemRepository receiptItemRepository,
                                 AdjustmentRepository adjustmentRepository,
                                 DebitNoteRepository debitNoteRepository,
                                 InventoryRepository inventoryRepository,
                                 ReceiptValidationService receiptValidationService,
                                 AuditLogService auditLogService) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.inventoryRepository = inventoryRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Creates an RTV (Return To Vendor) request for a QC_FAILED receipt.
     */
    @Transactional
    public RtvActionResponse createRtv(Long receiptId,
                                        ReceiptRtvCreateRequest request,
                                        User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "QUARANTINE_RTV_CREATE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        boolean isValidState = receipt.getStatus() == ReceiptStatus.QC_FAILED 
                || (receipt.getType() == ReceiptType.RETURN && receipt.getStatus() == ReceiptStatus.APPROVED);
        if (!isValidState) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: RTV can only be created for QC_FAILED receipts or APPROVED return receipts. "
                    + "Receipt " + receiptId + " has status: " + receipt.getStatus());
        }

        if (adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                RTV_REFERENCE_TYPE, receiptId, AdjustmentType.RETURN_TO_VENDOR)) {
            throw new RtvAlreadyExistsException(receiptId);
        }

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "NO_QUARANTINE_ITEMS: Receipt " + receiptId + " has no items to process for RTV.");
        }

        BigDecimal totalFailedQty = items.stream()
                .map(i -> i.getActualQty() != null ? BigDecimal.valueOf(i.getActualQty()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = i.getActualQty() != null ? BigDecimal.valueOf(i.getActualQty()) : BigDecimal.ZERO;
                    BigDecimal cost = i.getUnitCost() != null ? i.getUnitCost() : BigDecimal.ZERO;
                    return qty.multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate documentDate = request.getDocumentDate() != null
                ? request.getDocumentDate()
                : LocalDate.now();

        String adjustmentNumber = generateAdjustmentNumber();
        Adjustment rtv = Adjustment.builder()
                .adjustmentNumber(adjustmentNumber)
                .warehouse(receipt.getWarehouse())
                .product(items.get(0).getProduct())
                .quantityAdjustment(totalFailedQty.negate())
                .type(AdjustmentType.RETURN_TO_VENDOR)
                .referenceId(receiptId)
                .referenceType(RTV_REFERENCE_TYPE)
                .reason(request.getReason())
                .documentDate(documentDate)
                .createdBy(actor)
                .createdAt(OffsetDateTime.now())
                .build();
        adjustmentRepository.save(rtv);

        String debitNoteNumber = generateDebitNoteNumber();
        DebitNote debitNote = DebitNote.builder()
                .debitNoteNumber(debitNoteNumber)
                .supplier(receipt.getSupplier())
                .receipt(receipt)
                .failedQty(totalFailedQty)
                .amount(totalAmount)
                .reason(request.getReason())
                .createdBy(actor)
                .documentDate(documentDate)
                .createdAt(OffsetDateTime.now())
                .build();
        debitNoteRepository.save(debitNote);

        auditLogService.log(
                actor, AuditAction.QUARANTINE_RTV_CREATE, ADJUSTMENT_ENTITY,
                rtv.getId(), adjustmentNumber,
                receipt.getWarehouse().getId(),
                null,
                Map.of("receiptId", receiptId,
                       "adjustmentNumber", adjustmentNumber,
                       "debitNoteNumber", debitNoteNumber,
                       "failedQty", totalFailedQty,
                       "amount", totalAmount,
                       "inventoryDeducted", false)
        );

        log.info("RTV {} created for receipt {} by user {}. Debit Note {} generated.",
                adjustmentNumber, receiptId, actor.getId(), debitNoteNumber);

        return RtvActionResponse.builder()
                .adjustmentId(rtv.getId())
                .adjustmentNumber(adjustmentNumber)
                .debitNoteId(debitNote.getId())
                .debitNoteNumber(debitNoteNumber)
                .quarantineQty(totalFailedQty)
                .confirmed(false)
                .message("RTV request created. Debit Note generated. Awaiting physical return confirmation.")
                .build();
    }

    /**
     * Storekeeper confirms physical return of QC_FAILED goods to supplier.
     */
    @Transactional
    public RtvActionResponse confirmRtv(Long receiptId,
                                         ReceiptRtvConfirmRequest request,
                                         User actor) {
        receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "QUARANTINE_RTV_CONFIRM");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        if (adjustmentRepository.findConfirmedRtvByReference(
                RTV_REFERENCE_TYPE, receiptId, AdjustmentType.RETURN_TO_VENDOR).isPresent()) {
            throw new BusinessRuleViolationException(
                    "RTV_ALREADY_CONFIRMED: The Return-To-Vendor for receipt " + receiptId
                    + " has already been confirmed.");
        }

        Adjustment rtv = adjustmentRepository.findPendingRtvByReference(
                        RTV_REFERENCE_TYPE, receiptId, AdjustmentType.RETURN_TO_VENDOR)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending RTV adjustment found for receipt: " + receiptId));

        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        BigDecimal quarantineQty = items.stream()
                .map(i -> i.getActualQty() != null ? BigDecimal.valueOf(i.getActualQty()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.getReturnedQty().compareTo(quarantineQty) != 0) {
            throw new BusinessRuleViolationException(
                    "RTV_QUANTITY_MISMATCH: Returned quantity " + request.getReturnedQty()
                    + " does not equal the full quarantined quantity " + quarantineQty
                    + " for receipt " + receiptId + ". Partial RTV confirmation is not allowed.");
        }
        for (ReceiptItem item : items) {
            deductQuarantineInventory(receipt, item, actor);
        }

        rtv.setApprovedBy(actor);
        rtv.setApprovedAt(OffsetDateTime.now());
        adjustmentRepository.save(rtv);

        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.QUARANTINE_RTV_CONFIRM, ADJUSTMENT_ENTITY,
                rtv.getId(), rtv.getAdjustmentNumber(),
                receipt.getWarehouse().getId(),
                Map.of("confirmed", false, "quarantineQty", quarantineQty),
                Map.of("confirmed", true,
                       "confirmedBy", actor.getId(),
                       "confirmedAt", rtv.getApprovedAt().toString(),
                       "deductedQty", quarantineQty)
        );

        log.info("RTV {} confirmed for receipt {} by user {}. Quarantine qty {} deducted.",
                rtv.getAdjustmentNumber(), receiptId, actor.getId(), quarantineQty);

        return RtvActionResponse.builder()
                .adjustmentId(rtv.getId())
                .adjustmentNumber(rtv.getAdjustmentNumber())
                .quarantineQty(quarantineQty)
                .confirmed(true)
                .confirmedAt(rtv.getApprovedAt())
                .message("RTV confirmed. Quarantine inventory deducted.")
                .build();
    }



    private void deductQuarantineInventory(Receipt receipt, ReceiptItem item, User actor) {
        BigDecimal qty = item.getActualQty() != null ? BigDecimal.valueOf(item.getActualQty()) : BigDecimal.ZERO;
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (item.getLocation() == null) {
            log.warn("Receipt item {} has no location assigned; skipping quarantine deduction", item.getId());
            return;
        }

        Long warehouseId = receipt.getWarehouse().getId();
        Long productId = item.getProduct().getId();
        Long batchId = item.getBatch() != null ? item.getBatch().getId() : null;
        Long locationId = item.getLocation().getId();

        if (batchId == null) {
            throw new BusinessRuleViolationException(
                    "MISSING_BATCH: Cannot deduct quarantine inventory for item " + item.getId()
                    + " with no batch assigned.");
        }

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouseId, productId, batchId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quarantine inventory record not found for product " + productId
                        + " at location " + locationId));

        BigDecimal oldQty = inventory.getTotalQty();
        BigDecimal newQty = oldQty.subtract(qty);

        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException(
                    "INVENTORY_INVARIANT_VIOLATED: Deducting " + qty + " from quarantine inventory "
                    + "would result in negative total_qty (" + newQty + ") for product " + productId);
        }

        inventory.setTotalQty(newQty);
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);

        auditLogService.log(
                actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(),
                "INV-QUARANTINE-" + warehouseId + "-" + productId,
                warehouseId,
                Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", newQty, "reservedQty", inventory.getReservedQty(),
                       "delta", qty.negate(), "reason", "RTV_CONFIRM")
        );
    }

    @Transactional(readOnly = true)
    public List<QuarantineItemResponse> getQuarantineItems(Long warehouseId, User actor) {
        receiptValidationService.assertWarehouseAccess(actor, warehouseId);

        List<ReceiptItem> failedItems = receiptItemRepository.findQuarantineItemsByWarehouseId(warehouseId);

        return failedItems.stream()
                .map(item -> {
                    BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
                    BigDecimal failedQty = item.getSampleFailedQty() != null ? BigDecimal.valueOf(item.getSampleFailedQty()) : BigDecimal.ZERO;
                    BigDecimal totalValue = failedQty.multiply(unitCost);

                    return QuarantineItemResponse.builder()
                            .id(item.getId())
                            .productSku(item.getProduct().getSku())
                            .productName(item.getProduct().getName())
                            .qcFailedQty(item.getSampleFailedQty())
                            .qcFailureReason(item.getQcFailureReason())
                            .receiptNumber(item.getReceipt().getReceiptNumber())
                            .supplierId(item.getReceipt().getSupplier() != null ? item.getReceipt().getSupplier().getId() : null)
                            .totalValue(totalValue)
                            .unit("cái") // Default unit matching Sprint 1 context
                            .receiptId(item.getReceipt().getId())
                            .receiptVersion(item.getReceipt().getVersion())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateDebitNoteNumber() {
        return "DN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
