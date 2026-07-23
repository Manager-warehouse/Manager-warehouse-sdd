package com.wms.service.stock_receiving;
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

import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptItemQcResponse;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.repository.*;
import com.wms.service.audit_trail.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import com.wms.exception.BusinessRuleViolationException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptQcService {

    private static final int TRUSTED_SUPPLIER_THRESHOLD = 5;

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptValidationService receiptValidationService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;

    @Transactional
    public ReceiptQcResponse processQc(Long receiptId, ReceiptQcRequest request, String actorEmail) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        Receipt receipt = receiptValidationService.loadReceiptForUpdate(receiptId);
        receiptValidationService.assertWarehouseAssignment(actor, receiptId);

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new IllegalStateException("RECEIPT_NOT_IN_DRAFT");
        }

        return switch (request.getAction()) {
            case SUBMIT -> {
                receiptValidationService.assertRole(actor, UserRole.WAREHOUSE_STAFF, "RECEIPT_QC_SUBMIT");
                yield submitQc(receipt, request.getItems(), actor);
            }
            case CONFIRM -> {
                receiptValidationService.assertRole(actor, UserRole.STOREKEEPER, "RECEIPT_QC_CONFIRM");
                yield confirmQc(receipt, actor);
            }
        };
    }

    /** WAREHOUSE_STAFF ghi nháº­n káº¿t quáº£ QC máº«u cho tá»«ng item. */
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

            Integer passed = req.getQcPassedQty();
            Integer failed = req.getQcFailedQty();
            if (passed == null || failed == null) {
                throw new IllegalArgumentException("QC_QUANTITIES_REQUIRED for item " + req.getReceiptItemId());
            }
            Integer total = req.getSampleQty() != null ? req.getSampleQty() : item.getActualQty();
            if (total == null || passed + failed != total) {
                throw new IllegalArgumentException("QC_SAMPLE_SUM_MISMATCH for item " + req.getReceiptItemId());
            }

            item.setSampleQty(total);
            item.setSamplePassedQty(passed);
            item.setSampleFailedQty(failed);
            item.setQcSamplingMethod(req.getQcSamplingMethod() != null ? req.getQcSamplingMethod() : defaultMethod);
            item.setQcFailureReason(req.getQcFailureReason());
            item.setQcBy(actor);

            if (failed == 0) {
                item.setQcResult(QcResult.PASSED);
            } else {
                item.setQcResult(QcResult.FAILED);
            }

            receiptItemRepository.save(item);
        }

        auditLogService.log(
                actor, AuditAction.RECEIPT_QC_SUBMIT, "Receipt", receipt.getId(),
                receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null,
                Map.of("submittedItems", itemRequests.size())
        );

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
                .anyMatch(i -> i.getQcResult() == QcResult.FAILED);

        ReceiptStatus resultStatus = anyFailed ? ReceiptStatus.QC_FAILED : ReceiptStatus.QC_COMPLETED;
        receipt.setStatus(resultStatus);
        receiptRepository.save(receipt);

        if (anyFailed) {
            boolean hasFailedQty = items.stream().anyMatch(item -> item.getSampleFailedQty() != null && item.getSampleFailedQty() > 0);
            if (hasFailedQty) {
                WarehouseLocation quarantineLoc = warehouseLocationRepository
                        .findFirstByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(receipt.getWarehouse().getId())
                        .orElseThrow(() -> new BusinessRuleViolationException(
                                "QUARANTINE_LOCATION_NOT_CONFIGURED: Active quarantine location not found for warehouse " 
                                + receipt.getWarehouse().getId()));

            for (ReceiptItem item : items) {
                Integer failedQtyInt = item.getSampleFailedQty();
                if (failedQtyInt == null || failedQtyInt <= 0) {
                    continue;
                }
                BigDecimal failedQty = BigDecimal.valueOf(failedQtyInt);

                Batch batch = resolveOrCreateBatch(item, receipt);
                item.setBatch(batch);
                item.setLocation(quarantineLoc);
                receiptItemRepository.save(item);

                Inventory inventory = inventoryRepository
                        .findByWarehouseProductBatchLocationForUpdate(
                                receipt.getWarehouse().getId(), item.getProduct().getId(), batch.getId(), quarantineLoc.getId())
                        .orElseGet(() -> Inventory.builder()
                                .warehouse(receipt.getWarehouse())
                                .product(item.getProduct())
                                .batch(batch)
                                .location(quarantineLoc)
                                .totalQty(BigDecimal.ZERO)
                                .reservedQty(BigDecimal.ZERO)
                                .costPrice(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                                .updatedAt(OffsetDateTime.now())
                                .build());

                BigDecimal oldQty = inventory.getTotalQty();
                inventory.setTotalQty(oldQty.add(failedQty));
                inventory.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inventory);

                BigDecimal itemVol = item.getProduct().getVolumeM3() != null ? item.getProduct().getVolumeM3() : BigDecimal.ZERO;
                BigDecimal itemWt = item.getProduct().getWeightKg() != null ? item.getProduct().getWeightKg() : BigDecimal.ZERO;

                BigDecimal addedVol = failedQty.multiply(itemVol);
                BigDecimal addedWt = failedQty.multiply(itemWt);

                BigDecimal curVol = quarantineLoc.getCurrentVolumeM3() != null ? quarantineLoc.getCurrentVolumeM3() : BigDecimal.ZERO;
                BigDecimal curWt = quarantineLoc.getCurrentWeightKg() != null ? quarantineLoc.getCurrentWeightKg() : BigDecimal.ZERO;

                quarantineLoc.setCurrentVolumeM3(curVol.add(addedVol));
                quarantineLoc.setCurrentWeightKg(curWt.add(addedWt));
                quarantineLoc.setUpdatedAt(OffsetDateTime.now());
                warehouseLocationRepository.save(quarantineLoc);

                auditLogService.log(
                        actor, AuditAction.INVENTORY_UPDATE, "INVENTORY",
                        inventory.getId(),
                        "INV-QUARANTINE-" + receipt.getWarehouse().getId() + "-" + item.getProduct().getId(),
                        receipt.getWarehouse().getId(),
                        Map.of("totalQty", oldQty, "reservedQty", inventory.getReservedQty()),
                        Map.of("totalQty", inventory.getTotalQty(), "reservedQty", inventory.getReservedQty(),
                               "locationId", quarantineLoc.getId(), "delta", failedQty, "reason", "QC_FAILED")
                );
            }
        }
    }

        auditLogService.log(
                actor, AuditAction.RECEIPT_QC_CONFIRM, "Receipt", receipt.getId(),
                receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null,
                Map.of("result", resultStatus.name())
        );

        return buildResponse(receipt);
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
