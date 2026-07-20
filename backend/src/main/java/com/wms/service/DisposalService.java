package com.wms.service;

import com.wms.dto.request.DisposalRequest;
import com.wms.dto.response.DisposalResponse;
import com.wms.dto.response.PendingDisposalResponse;
import com.wms.entity.*;
import com.wms.enums.AdjustmentType;
import com.wms.enums.AuditAction;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisposalService {

    private static final Logger log = LoggerFactory.getLogger(DisposalService.class);

    private static final String DAMAGE_REPORT_ENTITY = "DAMAGE_REPORT";
    private static final String ADJUSTMENT_ENTITY = "ADJUSTMENT";
    private static final String INVENTORY_ENTITY = "INVENTORY";

    private final ReceiptItemRepository receiptItemRepository;
    private final DamageReportRepository damageReportRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;
    private final QuarantineRecordRepository quarantineRecordRepository;
    private final AccountingPeriodService accountingPeriodService;

    private static final BigDecimal AUTO_APPROVAL_THRESHOLD = new BigDecimal("5000000"); // 5,000,000 VND
    private static final BigDecimal CEO_APPROVAL_THRESHOLD = new BigDecimal("100000000"); // 100,000,000 VND

    public DisposalService(ReceiptItemRepository receiptItemRepository,
                           DamageReportRepository damageReportRepository,
                           AdjustmentRepository adjustmentRepository,
                           InventoryRepository inventoryRepository,
                           WarehouseLocationRepository warehouseLocationRepository,
                           PriceHistoryRepository priceHistoryRepository,
                           UserWarehouseAssignmentRepository userWarehouseAssignmentRepository,
                           ReceiptValidationService receiptValidationService,
                           AuditLogService auditLogService,
                           QuarantineRecordRepository quarantineRecordRepository,
                           AccountingPeriodService accountingPeriodService) {
        this.receiptItemRepository = receiptItemRepository;
        this.damageReportRepository = damageReportRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
        this.receiptValidationService = receiptValidationService;
        this.auditLogService = auditLogService;
        this.quarantineRecordRepository = quarantineRecordRepository;
        this.accountingPeriodService = accountingPeriodService;
    }

    @Transactional
    public DisposalResponse createDisposalRequest(Long receiptItemId, DisposalRequest request, User actor) {
        // Assert role WAREHOUSE_STAFF (which includes STOREKEEPER, WAREHOUSE_MANAGER, CEO, ADMIN)
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_STAFF, "DISPOSAL_CREATE");

        ReceiptItem receiptItem = receiptItemRepository.findById(receiptItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt item not found: " + receiptItemId));

        Receipt receipt = receiptItem.getReceipt();
        receiptValidationService.assertWarehouseAccess(actor, receipt.getWarehouse().getId());

        boolean isValidState = receipt.getStatus() == ReceiptStatus.QC_FAILED 
                || (receipt.getType() == com.wms.enums.ReceiptType.RETURN && receipt.getStatus() == ReceiptStatus.APPROVED);
        if (!isValidState) {
            throw new BusinessRuleViolationException(
                    "INVALID_STATE: Disposal can only be created for QC_FAILED receipts or APPROVED return receipts. Current status: " + receipt.getStatus());
        }

        if (receiptItem.getSampleFailedQty() == null || receiptItem.getSampleFailedQty() <= 0) {
            throw new BusinessRuleViolationException(
                    "NO_FAILED_QTY: Receipt item has no failed QC quantity to dispose");
        }

        // Check if a damage report already exists for this receipt item
        boolean reportExists = adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                "RECEIPT_ITEM", receiptItemId, AdjustmentType.DISPOSAL);
        if (reportExists) {
            throw new BusinessRuleViolationException("ALREADY_DISPOSED: A disposal adjustment has already been requested or processed for this item.");
        }

        // Determine unit cost and total value
        BigDecimal unitCost = receiptItem.getUnitCost();
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) == 0) {
            List<PriceHistory> prices = priceHistoryRepository.findLatestApproved(
                    receiptItem.getProduct().getId(), receipt.getWarehouse().getId());
            if (!prices.isEmpty()) {
                unitCost = prices.get(0).getCostPrice();
            } else {
                unitCost = BigDecimal.ZERO;
            }
        }

        BigDecimal failedQty = BigDecimal.valueOf(receiptItem.getSampleFailedQty());
        BigDecimal totalValue = failedQty.multiply(unitCost);

        // Generate DR Number
        String reportNumber = "DR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        DamageReport damageReport = DamageReport.builder()
                .reportNumber(reportNumber)
                .warehouse(receipt.getWarehouse())
                .product(receiptItem.getProduct())
                .batch(receiptItem.getBatch())
                .quantity(failedQty)
                .cause(request.getCause())
                .imageUrl(request.getImageUrl())
                .reportedBy(actor)
                .receiptItem(receiptItem)
                .reportDate(LocalDate.now())
                .createdAt(OffsetDateTime.now())
                .build();

        damageReport = damageReportRepository.save(damageReport);

        // Generate Adjustment Number
        String adjNumber = "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Adjustment adjustment = Adjustment.builder()
                .adjustmentNumber(adjNumber)
                .warehouse(receipt.getWarehouse())
                .product(receiptItem.getProduct())
                .batch(receiptItem.getBatch())
                .location(receiptItem.getLocation())
                .quantityAdjustment(failedQty.negate())
                .type(AdjustmentType.DISPOSAL)
                .referenceId(receiptItemId) // FE maps referenceId to receiptItemId in display
                .referenceType("RECEIPT_ITEM")
                .reason(request.getCause())
                .createdBy(actor)
                .documentDate(LocalDate.now())
                .accountingPeriod(accountingPeriodService.resolveOpenPeriod(LocalDate.now()))
                .createdAt(OffsetDateTime.now())
                .build();

        boolean autoApproved = totalValue.compareTo(AUTO_APPROVAL_THRESHOLD) < 0;
        if (autoApproved) {
            adjustment.setApprovedBy(actor);
            adjustment.setApprovedAt(OffsetDateTime.now());
            adjustment = adjustmentRepository.save(adjustment);

            // Deduct quarantine inventory immediately
            deductQuarantineInventory(adjustment, actor);

            auditLogService.log(
                    actor, AuditAction.QUARANTINE_DISPOSAL_APPROVE, DAMAGE_REPORT_ENTITY,
                    damageReport.getId(), reportNumber,
                    receipt.getWarehouse().getId(),
                    Map.of(),
                    Map.of("autoApproved", true, "totalValue", totalValue, "adjustmentId", adjustment.getId())
            );
        } else {
            adjustment = adjustmentRepository.save(adjustment);

            auditLogService.log(
                    actor, AuditAction.QUARANTINE_DISPOSAL_CREATE, DAMAGE_REPORT_ENTITY,
                    damageReport.getId(), reportNumber,
                    receipt.getWarehouse().getId(),
                    Map.of(),
                    Map.of("autoApproved", false, "totalValue", totalValue, "adjustmentId", adjustment.getId())
            );
        }

        return DisposalResponse.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .autoApproved(autoApproved)
                .message(autoApproved 
                        ? "Đã tạo phiếu tiêu hủy tự động duyệt (Giá trị: " + totalValue + " VND)"
                        : "Đã tạo yêu cầu tiêu hủy chờ phê duyệt (Giá trị: " + totalValue + " VND)")
                .build();
    }

    @Transactional
    public DisposalResponse createDisposalFromQuarantine(Long quarantineRecordId, DisposalRequest request, User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_STAFF, "DISPOSAL_CREATE");

        QuarantineRecord qr = quarantineRecordRepository.findById(quarantineRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Quarantine record not found: " + quarantineRecordId));

        receiptValidationService.assertWarehouseAccess(actor, qr.getWarehouse().getId());

        if (qr.getRemainingQuantity() == null || qr.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("NO_FAILED_QTY: Quarantine record has no quantity to dispose");
        }

        // Chặn không cho phép tạo RTV/Debit Note cho hàng điều chuyển
        if ("INTERNAL_TRANSFER".equals(qr.getOriginType())) {
            // Check report already exists
            boolean reportExists = adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                    "QUARANTINE_RECORD", quarantineRecordId, AdjustmentType.DISPOSAL);
            if (reportExists) {
                throw new BusinessRuleViolationException("ALREADY_DISPOSED: A disposal adjustment has already been requested or processed for this item.");
            }
        }

        BigDecimal unitCost = BigDecimal.ZERO;
        List<PriceHistory> prices = priceHistoryRepository.findLatestApproved(
                qr.getProduct().getId(), qr.getWarehouse().getId());
        if (!prices.isEmpty()) {
            unitCost = prices.get(0).getCostPrice();
        }

        BigDecimal failedQty = qr.getRemainingQuantity();
        BigDecimal totalValue = failedQty.multiply(unitCost);

        String reportNumber = "DR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        DamageReport damageReport = DamageReport.builder()
                .reportNumber(reportNumber)
                .warehouse(qr.getWarehouse())
                .product(qr.getProduct())
                .batch(qr.getBatch())
                .quantity(failedQty)
                .cause(request.getCause())
                .imageUrl(request.getImageUrl())
                .reportedBy(actor)
                .reportDate(LocalDate.now())
                .createdAt(OffsetDateTime.now())
                .build();

        damageReport = damageReportRepository.save(damageReport);

        String adjNumber = "ADJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Adjustment adjustment = Adjustment.builder()
                .adjustmentNumber(adjNumber)
                .warehouse(qr.getWarehouse())
                .product(qr.getProduct())
                .batch(qr.getBatch())
                .location(qr.getLocation())
                .quantityAdjustment(failedQty.negate())
                .type(AdjustmentType.DISPOSAL)
                .referenceId(quarantineRecordId)
                .referenceType("QUARANTINE_RECORD")
                .quarantineRecord(qr)
                .reason(request.getCause())
                .createdBy(actor)
                .documentDate(LocalDate.now())
                .accountingPeriod(accountingPeriodService.resolveOpenPeriod(LocalDate.now()))
                .createdAt(OffsetDateTime.now())
                .build();

        boolean autoApproved = totalValue.compareTo(AUTO_APPROVAL_THRESHOLD) < 0;
        if (autoApproved) {
            adjustment.setApprovedBy(actor);
            adjustment.setApprovedAt(OffsetDateTime.now());
            adjustment = adjustmentRepository.save(adjustment);

            deductQuarantineInventory(adjustment, actor);

            qr.setRemainingQuantity(BigDecimal.ZERO);
            quarantineRecordRepository.save(qr);

            auditLogService.log(
                    actor, AuditAction.QUARANTINE_DISPOSAL_APPROVE, DAMAGE_REPORT_ENTITY,
                    damageReport.getId(), reportNumber,
                    qr.getWarehouse().getId(),
                    Map.of(),
                    Map.of("autoApproved", true, "totalValue", totalValue, "adjustmentId", adjustment.getId())
            );
        } else {
            adjustment = adjustmentRepository.save(adjustment);

            auditLogService.log(
                    actor, AuditAction.QUARANTINE_DISPOSAL_CREATE, DAMAGE_REPORT_ENTITY,
                    damageReport.getId(), reportNumber,
                    qr.getWarehouse().getId(),
                    Map.of(),
                    Map.of("autoApproved", false, "totalValue", totalValue, "adjustmentId", adjustment.getId())
            );
        }

        return DisposalResponse.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .autoApproved(autoApproved)
                .message(autoApproved 
                        ? "Đã tạo phiếu tiêu hủy tự động duyệt (Giá trị: " + totalValue + " VND)"
                        : "Đã tạo yêu cầu tiêu hủy chờ phê duyệt (Giá trị: " + totalValue + " VND)")
                .build();
    }

    @Transactional(readOnly = true)
    public List<PendingDisposalResponse> getPendingDisposals(User actor) {
        List<Adjustment> pendingAdjustments = adjustmentRepository.findByTypeAndApprovedAtIsNull(AdjustmentType.DISPOSAL);

        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.CEO) {
            List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(actor.getId());
            pendingAdjustments = pendingAdjustments.stream()
                    .filter(adj -> assignedWarehouseIds.contains(adj.getWarehouse().getId()))
                    .collect(Collectors.toList());
        }

        List<PendingDisposalResponse> responses = new ArrayList<>();
        for (Adjustment adj : pendingAdjustments) {
            BigDecimal failedQty = adj.getQuantityAdjustment().abs();
            BigDecimal unitCost = BigDecimal.ZERO;

            if (adj.getReferenceId() != null) {
                if ("RECEIPT_ITEM".equals(adj.getReferenceType())) {
                    var receiptItemOpt = receiptItemRepository.findById(adj.getReferenceId());
                    if (receiptItemOpt.isPresent()) {
                        BigDecimal cost = receiptItemOpt.get().getUnitCost();
                        if (cost != null) {
                            unitCost = cost;
                        }
                    }
                }
            }

            if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
                List<PriceHistory> prices = priceHistoryRepository.findLatestApproved(
                        adj.getProduct().getId(), adj.getWarehouse().getId());
                if (!prices.isEmpty()) {
                    unitCost = prices.get(0).getCostPrice();
                }
            }

            BigDecimal totalValue = failedQty.multiply(unitCost);

            responses.add(PendingDisposalResponse.builder()
                    .id(adj.getId())
                    .adjustmentNumber(adj.getAdjustmentNumber())
                    .warehouseId(adj.getWarehouse().getId())
                    .warehouseName(adj.getWarehouse().getName())
                    .productId(adj.getProduct().getId())
                    .productSku(adj.getProduct().getSku())
                    .productName(adj.getProduct().getName())
                    .failedQty(failedQty)
                    .totalValue(totalValue)
                    .cause(adj.getReason())
                    .reportedByName(adj.getCreatedBy().getFullName())
                    .documentDate(adj.getDocumentDate())
                    .createdAt(adj.getCreatedAt())
                    .build());
        }

        return responses;
    }

    @Transactional
    public DisposalResponse approveDisposal(Long adjustmentId, User actor) {
        receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_MANAGER, "DISPOSAL_APPROVE");

        Adjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found: " + adjustmentId));

        if (adjustment.getType() != AdjustmentType.DISPOSAL) {
            throw new BusinessRuleViolationException("INVALID_TYPE: Adjustment is not of type DISPOSAL");
        }

        if (adjustment.getApprovedAt() != null) {
            throw new BusinessRuleViolationException("ALREADY_APPROVED: This disposal has already been approved");
        }

        receiptValidationService.assertWarehouseAccess(actor, adjustment.getWarehouse().getId());

        BigDecimal failedQty = adjustment.getQuantityAdjustment().abs();
        BigDecimal unitCost = BigDecimal.ZERO;

        if (adjustment.getReferenceId() != null) {
            if ("RECEIPT_ITEM".equals(adjustment.getReferenceType())) {
                var receiptItemOpt = receiptItemRepository.findById(adjustment.getReferenceId());
                if (receiptItemOpt.isPresent()) {
                    BigDecimal cost = receiptItemOpt.get().getUnitCost();
                    if (cost != null) {
                        unitCost = cost;
                    }
                }
            }
        }

        if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
            List<PriceHistory> prices = priceHistoryRepository.findLatestApproved(
                    adjustment.getProduct().getId(), adjustment.getWarehouse().getId());
            if (!prices.isEmpty()) {
                unitCost = prices.get(0).getCostPrice();
            }
        }

        BigDecimal totalValue = failedQty.multiply(unitCost);

        if (totalValue.compareTo(CEO_APPROVAL_THRESHOLD) > 0) {
            if (actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
                throw new ForbiddenReceiptWarehouseException(
                        "FORBIDDEN_RECEIPT_ROLE: CEO approval is required for disposal values exceeding 100M VND. Value: " + totalValue);
            }
        }

        adjustment.setApprovedBy(actor);
        adjustment.setApprovedAt(OffsetDateTime.now());
        adjustmentRepository.save(adjustment);

        deductQuarantineInventory(adjustment, actor);

        if ("QUARANTINE_RECORD".equals(adjustment.getReferenceType())) {
            QuarantineRecord qr = quarantineRecordRepository.findById(adjustment.getReferenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quarantine record not found"));
            qr.setRemainingQuantity(qr.getRemainingQuantity().subtract(failedQty).max(BigDecimal.ZERO));
            quarantineRecordRepository.save(qr);
        }

        auditLogService.log(
                actor, AuditAction.QUARANTINE_DISPOSAL_APPROVE, ADJUSTMENT_ENTITY,
                adjustment.getId(), adjustment.getAdjustmentNumber(),
                adjustment.getWarehouse().getId(),
                Map.of("approved", "false"),
                Map.of("approved", "true", "approvedBy", actor.getId(), "approvedAt", adjustment.getApprovedAt().toString())
        );

        return DisposalResponse.builder()
                .adjustmentId(adjustment.getId())
                .adjustmentNumber(adjustment.getAdjustmentNumber())
                .autoApproved(false)
                .message("Phê duyệt thành công yêu cầu tiêu hủy: " + adjustment.getAdjustmentNumber())
                .build();
    }

    private void deductQuarantineInventory(Adjustment adjustment, User actor) {
        BigDecimal failedQty = adjustment.getQuantityAdjustment().abs();
        Long warehouseId = adjustment.getWarehouse().getId();
        Long productId = adjustment.getProduct().getId();
        Long batchId = adjustment.getBatch() != null ? adjustment.getBatch().getId() : null;
        Long locationId = adjustment.getLocation() != null ? adjustment.getLocation().getId() : null;

        if (locationId == null || batchId == null) {
            throw new BusinessRuleViolationException("MISSING_STOCK_KEYS: Batch and location are required for inventory deduction");
        }

        Inventory inventory = inventoryRepository
                .findByWarehouseProductBatchLocationForUpdate(warehouseId, productId, batchId, locationId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "INSUFFICIENT_STOCK: Quarantine stock record not found for product " + productId));

        if (inventory.getTotalQty().compareTo(failedQty) < 0) {
            throw new BusinessRuleViolationException(
                    "INSUFFICIENT_STOCK: Insufficient quarantine stock. Available: " 
                    + inventory.getTotalQty() + ", Required: " + failedQty);
        }

        BigDecimal oldQty = inventory.getTotalQty();
        inventory.setTotalQty(oldQty.subtract(failedQty));
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);

        // Update location capacity
        WarehouseLocation location = adjustment.getLocation();
        BigDecimal itemVol = adjustment.getProduct().getVolumeM3() != null ? adjustment.getProduct().getVolumeM3() : BigDecimal.ZERO;
        BigDecimal itemWt = adjustment.getProduct().getWeightKg() != null ? adjustment.getProduct().getWeightKg() : BigDecimal.ZERO;

        BigDecimal deductVol = failedQty.multiply(itemVol);
        BigDecimal deductWt = failedQty.multiply(itemWt);

        BigDecimal curVol = location.getCurrentVolumeM3() != null ? location.getCurrentVolumeM3() : BigDecimal.ZERO;
        BigDecimal curWt = location.getCurrentWeightKg() != null ? location.getCurrentWeightKg() : BigDecimal.ZERO;

        location.setCurrentVolumeM3(curVol.subtract(deductVol).max(BigDecimal.ZERO));
        location.setCurrentWeightKg(curWt.subtract(deductWt).max(BigDecimal.ZERO));
        location.setUpdatedAt(OffsetDateTime.now());
        warehouseLocationRepository.save(location);

        // Audit Log
        auditLogService.log(
                actor, AuditAction.INVENTORY_UPDATE, INVENTORY_ENTITY,
                inventory.getId(),
                "INV-" + warehouseId + "-" + productId,
                warehouseId,
                Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                Map.of("totalQty", inventory.getTotalQty(), "reservedQty", inventory.getReservedQty(),
                       "locationId", locationId, "delta", failedQty.negate())
        );
    }
}
