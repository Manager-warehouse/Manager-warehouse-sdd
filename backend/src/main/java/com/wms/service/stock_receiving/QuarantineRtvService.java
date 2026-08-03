package com.wms.service.stock_receiving;

import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.dto.response.QuarantineItemResponse;
import com.wms.dto.response.RtvActionResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.DebitNote;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.stock_receiving.QuarantineRecord;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_control.AdjustmentType;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.RtvAlreadyExistsException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DebitNoteRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.stock_receiving.QuarantineRecordRepository;
import com.wms.repository.stock_receiving.ReceiptItemRepository;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final QuarantineRecordRepository quarantineRecordRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final WarehouseLocationRepository warehouseLocationRepository;

    public QuarantineRtvService(ReceiptRepository receiptRepository,
                                 ReceiptItemRepository receiptItemRepository,
                                 AdjustmentRepository adjustmentRepository,
                                 DebitNoteRepository debitNoteRepository,
                                 InventoryRepository inventoryRepository,
                                 ReceiptValidationService receiptValidationService,
                                 AuditLogService auditLogService,
                                 QuarantineRecordRepository quarantineRecordRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 AccountingPeriodService accountingPeriodService) {
        this(receiptRepository, receiptItemRepository, adjustmentRepository, debitNoteRepository,
             inventoryRepository, receiptValidationService, auditLogService, quarantineRecordRepository,
             priceHistoryRepository, accountingPeriodService, null);
    }

    @Autowired
    public QuarantineRtvService(ReceiptRepository receiptRepository,
                                 ReceiptItemRepository receiptItemRepository,
                                 AdjustmentRepository adjustmentRepository,
                                 DebitNoteRepository debitNoteRepository,
                                 InventoryRepository inventoryRepository,
                                 ReceiptValidationService receiptValidationService,
                                 AuditLogService auditLogService,
                                 QuarantineRecordRepository quarantineRecordRepository,
                                 PriceHistoryRepository priceHistoryRepository,
                                 AccountingPeriodService accountingPeriodService,
                                 WarehouseLocationRepository warehouseLocationRepository) {
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.debitNoteRepository = debitNoteRepository;
        this.inventoryRepository = inventoryRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
        this.quarantineRecordRepository = quarantineRecordRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.warehouseLocationRepository = warehouseLocationRepository;
    }

    /**
     * Creates an RTV (Return To Vendor) request for finalized unresolved quarantine stock.
     */
    @Transactional
    public RtvActionResponse createRtv(Long receiptId,
                                        ReceiptRtvCreateRequest request,
                                        User actor) {
        receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "QUARANTINE_RTV_CREATE");
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);
        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        boolean isValidState = receipt.getStatus() == ReceiptStatus.PARTIALLY_APPROVED
                || receipt.getStatus() == ReceiptStatus.PUTAWAY_COMPLETED
                || receipt.getStatus() == ReceiptStatus.RETURN_TO_SUPPLIER_PENDING
                || (receipt.getType() == ReceiptType.RETURN && receipt.getStatus() == ReceiptStatus.APPROVED);
        if (!isValidState) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: RTV can only be created for finalized quarantine receipts or APPROVED return receipts. "
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
                .map(this::unresolvedQuarantineQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalFailedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException(
                    "NO_QUARANTINE_ITEMS: Receipt " + receiptId + " has no unresolved finalized quarantine quantity.");
        }
        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal qty = unresolvedQuarantineQty(i);
                    BigDecimal cost = i.getUnitCost() != null ? i.getUnitCost() : BigDecimal.ZERO;
                    return qty.multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate documentDate = request.getDocumentDate() != null
                ? request.getDocumentDate()
                : LocalDate.now();

        String firstAdjNumber = null;
        Long firstAdjId = null;
        for (ReceiptItem item : items) {
            BigDecimal itemFailedQty = unresolvedQuarantineQty(item);
            if (itemFailedQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            String adjustmentNumber = generateAdjustmentNumber();
            if (firstAdjNumber == null) firstAdjNumber = adjustmentNumber;

            Adjustment rtv = Adjustment.builder()
                    .adjustmentNumber(adjustmentNumber)
                    .warehouse(receipt.getWarehouse())
                    .product(item.getProduct())
                    .quantityAdjustment(itemFailedQty.negate())
                    .type(AdjustmentType.RETURN_TO_VENDOR)
                    .referenceId(receiptId)
                    .referenceType(RTV_REFERENCE_TYPE)
                    .reason(request.getReason())
                    .documentDate(documentDate)
                    .accountingPeriod(accountingPeriodService.resolveOpenPeriod(documentDate))
                    .createdBy(actor)
                    .createdAt(OffsetDateTime.now())
                    .build();
            adjustmentRepository.save(rtv);
            if (firstAdjId == null) firstAdjId = rtv.getId();
        }

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

        receipt.setStatus(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING);
        receipt.setUpdatedAt(OffsetDateTime.now());
        receiptRepository.save(receipt);

        auditLogService.log(
                actor, AuditAction.QUARANTINE_RTV_CREATE, ADJUSTMENT_ENTITY,
                firstAdjId, firstAdjNumber,
                receipt.getWarehouse().getId(),
                null,
                Map.of("receiptId", receiptId,
                       "adjustmentNumber", firstAdjNumber,
                       "debitNoteNumber", debitNoteNumber,
                       "failedQty", totalFailedQty,
                       "amount", totalAmount,
                       "inventoryDeducted", false)
        );

        log.info("RTV {} created for receipt {} by user {}. Debit Note {} generated.",
                firstAdjNumber, receiptId, actor.getId(), debitNoteNumber);

        return RtvActionResponse.builder()
                .adjustmentId(firstAdjId)
                .adjustmentNumber(firstAdjNumber)
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
        // Must match the quantity actually held in quarantine (sampleFailedQty),
        // not the full received actualQty — see createRtv() for the same fix.
        BigDecimal quarantineQty = items.stream()
                .map(this::unresolvedQuarantineQty)
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
        receiptItemRepository.saveAll(items);

        rtv.setApprovedBy(actor);
        rtv.setApprovedAt(OffsetDateTime.now());
        adjustmentRepository.save(rtv);

        receipt.setStatus(ReceiptStatus.RETURNED_TO_SUPPLIER);
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
        // Only the QC-failed portion of this item ever entered quarantine
        // inventory (see ReceiptQcService.confirmQc); deducting actualQty here
        // would try to remove more than what was ever added.
        BigDecimal qty = unresolvedQuarantineQty(item);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long warehouseId = receipt.getWarehouse().getId();
        Long productId = item.getProduct().getId();
        Long batchId = item.getBatch() != null ? item.getBatch().getId() : null;

        WarehouseLocation location = item.getLocation();
        if ((location == null || !Boolean.TRUE.equals(location.getIsQuarantine())) && warehouseLocationRepository != null) {
            location = warehouseLocationRepository
                    .findFirstByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(warehouseId)
                    .orElse(location);
        }

        if (location == null) {
            log.warn("Receipt item {} has no location assigned; skipping quarantine deduction", item.getId());
            return;
        }

        Long locationId = location.getId();

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
        item.setResolvedQuarantineQty(safe(item.getResolvedQuarantineQty()) + qty.intValue());

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
        List<QuarantineRecord> quarantineRecords = quarantineRecordRepository
                .findByWarehouseIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(warehouseId, BigDecimal.ZERO)
                .stream()
                .filter(qr -> !adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                        "QUARANTINE_RECORD", qr.getId(), com.wms.enums.stock_control.AdjustmentType.DISPOSAL))
                .collect(java.util.stream.Collectors.toList());

        List<QuarantineItemResponse> responses = new java.util.ArrayList<>();

        // Map Receipt QC failed items
        for (ReceiptItem item : failedItems) {
            BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
            BigDecimal failedQty = unresolvedQuarantineQty(item);
            BigDecimal totalValue = failedQty.multiply(unitCost);
            String originType = item.getReceipt().getType() == ReceiptType.RETURN ? "DEALER_RETURN" : "RECEIPT";

            Long dealerId = item.getReceipt().getDealer() != null ? item.getReceipt().getDealer().getId() : null;
            String dealerName = item.getReceipt().getDealer() != null ? item.getReceipt().getDealer().getName() : null;

            responses.add(QuarantineItemResponse.builder()
                    .id(item.getId())
                    .productSku(item.getProduct().getSku())
                    .productName(item.getProduct().getName())
                    .qcFailedQty(failedQty.intValue())
                    .qcFailureReason(item.getQcFailureReason())
                    .receiptNumber(item.getReceipt().getReceiptNumber())
                    .supplierId(item.getReceipt().getSupplier() != null ? item.getReceipt().getSupplier().getId() : null)
                    .dealerId(dealerId)
                    .dealerName(dealerName)
                    .totalValue(totalValue)
                    .unit(item.getProduct().getUnit() != null ? item.getProduct().getUnit() : "cái")
                    .receiptId(item.getReceipt().getId())
                    .receiptVersion(item.getReceipt().getVersion())
                    .originType(originType)
                    .build());
        }

        // Map Quarantine Records (such as internal transfers)
        for (QuarantineRecord qr : quarantineRecords) {
            BigDecimal unitCost = quarantineRecordUnitCost(qr);

            BigDecimal failedQty = qr.getRemainingQuantity();
            BigDecimal totalValue = failedQty.multiply(unitCost);

            responses.add(QuarantineItemResponse.builder()
                    .id(qr.getId())
                    .productSku(qr.getProduct().getSku())
                    .productName(qr.getProduct().getName())
                    .qcFailedQty(failedQty.intValue())
                    .qcFailureReason(qr.getReason())
                    .receiptNumber(qr.getTransfer() != null ? qr.getTransfer().getTransferNumber() : "N/A")
                    .supplierId(null)
                    .totalValue(totalValue)
                    .unit(qr.getProduct().getUnit() != null ? qr.getProduct().getUnit() : "cái")
                    .receiptId(null)
                    .receiptVersion(0)
                    .originType(qr.getOriginType())
                    .quarantineRecordId(qr.getId())
                    .build());
        }

        return responses;
    }

    private BigDecimal quarantineRecordUnitCost(QuarantineRecord qr) {
        return inventoryRepository.findByWarehouseProductBatchLocation(
                        qr.getWarehouse().getId(),
                        qr.getProduct().getId(),
                        qr.getBatch().getId(),
                        qr.getLocation().getId())
                .map(Inventory::getCostPrice)
                .filter(cost -> cost != null && cost.compareTo(BigDecimal.ZERO) > 0)
                .orElseGet(() -> latestApprovedCost(qr));
    }

    private BigDecimal latestApprovedCost(QuarantineRecord qr) {
        List<PriceHistory> prices = priceHistoryRepository.findLatestApproved(
                qr.getProduct().getId(), qr.getWarehouse().getId());
        return prices.isEmpty() ? BigDecimal.ZERO : prices.get(0).getCostPrice();
    }

    private String generateAdjustmentNumber() {
        return "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateDebitNoteNumber() {
        return "DN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BigDecimal unresolvedQuarantineQty(ReceiptItem item) {
        int quantity = safe(item.getQuarantineQty()) - safe(item.getResolvedQuarantineQty());
        if (quantity <= 0
                && item.getReceipt() != null
                && item.getReceipt().getType() == ReceiptType.RETURN) {
            quantity = safe(item.getSampleFailedQty());
        }
        return BigDecimal.valueOf(Math.max(quantity, 0));
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }
}
