package com.wms.service;

import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptItemQcResponse;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            Integer total = req.getSampleQty() != null ? req.getSampleQty() : (passed + failed);
            if (passed + failed != total) {
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

    /** STOREKEEPER káº¿t luáº­n QC: chuyá»ƒn tráº¡ng thÃ¡i receipt vÃ  xá»­ lÃ½ quarantine náº¿u lá»—i. */
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
        auditLogService.log(
                actor, AuditAction.RECEIPT_QC_CONFIRM, "Receipt", receipt.getId(),
                receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null,
                Map.of("result", resultStatus.name())
        );

        return buildResponse(receipt);
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
