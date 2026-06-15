package com.wms.service;

import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptItemQcResponse;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.repository.*;
import com.wms.util.AuditLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptQcService {

    private static final int TRUSTED_SUPPLIER_THRESHOLD = 5;

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final WarehouseLocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReceiptQcResponse processQc(Long receiptId, ReceiptQcRequest request, String actorEmail) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("RECEIPT_NOT_FOUND"));

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new IllegalStateException("RECEIPT_NOT_IN_DRAFT");
        }

        return switch (request.getAction()) {
            case SUBMIT -> submitQc(receipt, request.getItems(), actor);
            case CONFIRM -> confirmQc(receipt, actor);
        };
    }

    /** WAREHOUSE_STAFF ghi nhận kết quả QC mẫu cho từng item. */
    private ReceiptQcResponse submitQc(Receipt receipt, List<ReceiptQcItemRequest> itemRequests, User actor) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("QC_ITEMS_REQUIRED");
        }

        Long supplierId = receipt.getSupplier() != null ? receipt.getSupplier().getId() : null;
        long approvedCount = supplierId != null
                ? receiptItemRepository.countApprovedReceiptsBySupplierId(supplierId)
                : 0;
        QcSamplingMethod defaultMethod = approvedCount >= TRUSTED_SUPPLIER_THRESHOLD
                ? QcSamplingMethod.RANDOM_SAMPLE
                : QcSamplingMethod.FULL_INSPECTION;

        for (ReceiptQcItemRequest req : itemRequests) {
            ReceiptItem item = receiptItemRepository.findByIdAndReceiptId(req.getReceiptItemId(), receipt.getId())
                    .orElseThrow(() -> new IllegalArgumentException("RECEIPT_ITEM_NOT_FOUND: " + req.getReceiptItemId()));

            BigDecimal passed = req.getSamplePassedQty();
            BigDecimal failed = req.getSampleFailedQty();
            BigDecimal total = req.getSampleQty();
            if (passed.add(failed).compareTo(total) != 0) {
                throw new IllegalArgumentException("QC_SAMPLE_SUM_MISMATCH for item " + req.getReceiptItemId());
            }

            item.setSampleQty(total);
            item.setSamplePassedQty(passed);
            item.setSampleFailedQty(failed);
            item.setQcSamplingMethod(req.getQcSamplingMethod() != null ? req.getQcSamplingMethod() : defaultMethod);
            item.setQcFailureReason(req.getQcFailureReason());
            item.setQcBy(actor);

            if (failed.compareTo(BigDecimal.ZERO) == 0) {
                item.setQcResult(QcResult.PASSED);
            } else if (passed.compareTo(BigDecimal.ZERO) == 0) {
                item.setQcResult(QcResult.FAILED);
            } else {
                item.setQcResult(QcResult.PARTIAL);
            }

            receiptItemRepository.save(item);
        }

        saveAuditLog(actor, AuditAction.RECEIPT_QC_SUBMIT, receipt,
                Map.of("submittedItems", itemRequests.size()));

        return buildResponse(receipt);
    }

    /** STOREKEEPER kết luận QC: chuyển trạng thái receipt và xử lý quarantine nếu lỗi. */
    private ReceiptQcResponse confirmQc(Receipt receipt, User actor) {
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receipt.getId());

        boolean anyPending = items.stream().anyMatch(i -> i.getQcResult() == null || i.getQcResult() == QcResult.PENDING);
        if (anyPending) {
            throw new IllegalStateException("QC_NOT_YET_SUBMITTED");
        }

        boolean anyFailed = items.stream()
                .anyMatch(i -> i.getQcResult() == QcResult.FAILED || i.getQcResult() == QcResult.PARTIAL);

        if (!anyFailed) {
            receipt.setStatus(ReceiptStatus.QC_COMPLETED);
            receiptRepository.save(receipt);
            saveAuditLog(actor, AuditAction.RECEIPT_QC_CONFIRM, receipt,
                    Map.of("result", "QC_COMPLETED"));
        } else {
            receipt.setStatus(ReceiptStatus.QC_FAILED);
            receiptRepository.save(receipt);
            moveFailedLotsToQuarantine(receipt, items);
            saveAuditLog(actor, AuditAction.RECEIPT_QC_CONFIRM, receipt,
                    Map.of("result", "QC_FAILED"));
        }

        return buildResponse(receipt);
    }

    /**
     * Khi QC lỗi: tạo Batch cấp C cho từng item và chuyển toàn bộ actual_qty vào quarantine inventory.
     */
    private void moveFailedLotsToQuarantine(Receipt receipt, List<ReceiptItem> items) {
        WarehouseLocation quarantine = locationRepository
                .findFirstByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(receipt.getWarehouse().getId())
                .orElseThrow(() -> new IllegalStateException("QUARANTINE_LOCATION_NOT_FOUND"));

        for (ReceiptItem item : items) {
            if (item.getActualQty() == null || item.getActualQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            String batchNumber = "QC-FAIL-" + receipt.getReceiptNumber() + "-" + item.getId();
            Batch batch = batchRepository.findByBatchNumber(batchNumber).orElseGet(() ->
                    batchRepository.save(Batch.builder()
                            .batchNumber(batchNumber)
                            .product(item.getProduct())
                            .warehouse(receipt.getWarehouse())
                            .receivedDate(LocalDate.now())
                            .grade(BatchGrade.C)
                            .quantity(item.getActualQty())
                            .createdAt(OffsetDateTime.now())
                            .build()));

            // Link batch to receipt item for traceability
            item.setBatch(batch);
            receiptItemRepository.save(item);

            // Upsert quarantine inventory
            Inventory inv = inventoryRepository
                    .findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
                            receipt.getWarehouse().getId(),
                            item.getProduct().getId(),
                            batch.getId(),
                            quarantine.getId())
                    .orElseGet(() -> Inventory.builder()
                            .warehouse(receipt.getWarehouse())
                            .product(item.getProduct())
                            .batch(batch)
                            .location(quarantine)
                            .totalQty(BigDecimal.ZERO)
                            .reservedQty(BigDecimal.ZERO)
                            .costPrice(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                            .version(0)
                            .updatedAt(OffsetDateTime.now())
                            .build());

            inv.setTotalQty(inv.getTotalQty().add(item.getActualQty()));
            inv.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inv);
        }
    }

    private void saveAuditLog(User actor, AuditAction action, Receipt receipt, Map<String, Object> details) {
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .actorRole(actor.getRole().name())
                .action(action)
                .entityType("Receipt")
                .entityId(receipt.getId())
                .warehouse(receipt.getWarehouse())
                .description(action.name() + " Receipt " + receipt.getReceiptNumber())
                .newValue(AuditLogUtil.toJson(details))
                .timestamp(OffsetDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    private ReceiptQcResponse buildResponse(Receipt receipt) {
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receipt.getId());
        List<ReceiptItemQcResponse> itemResponses = items.stream()
                .map(i -> ReceiptItemQcResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .expectedQty(i.getExpectedQty())
                        .actualQty(i.getActualQty())
                        .sampleQty(i.getSampleQty())
                        .samplePassedQty(i.getSamplePassedQty())
                        .sampleFailedQty(i.getSampleFailedQty())
                        .qcSamplingMethod(i.getQcSamplingMethod())
                        .qcResult(i.getQcResult())
                        .qcFailureReason(i.getQcFailureReason())
                        .qcByUserId(i.getQcBy() != null ? i.getQcBy().getId() : null)
                        .build())
                .collect(Collectors.toList());

        return ReceiptQcResponse.builder()
                .receiptId(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .items(itemResponses)
                .build();
    }
}
