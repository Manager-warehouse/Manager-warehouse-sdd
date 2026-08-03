package com.wms.service.stock_receiving;

import com.wms.dto.request.stock_receiving.ReceiptQcItemRequest;
import com.wms.dto.request.stock_receiving.ReceiptQcRequest;
import com.wms.dto.response.stock_receiving.ReceiptItemQcResponse;
import com.wms.dto.response.stock_receiving.ReceiptQcResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_receiving.QcResult;
import com.wms.enums.stock_receiving.QcSamplingMethod;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.stock_receiving.ReceiptItemRepository;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.repository.UserRepository;
import com.wms.service.audit_trail.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
        receiptValidationService.assertVersionMatch(receipt, request.getExpectedVersion());

        if (receipt.getStatus() == ReceiptStatus.PENDING_MANAGER_APPROVAL
                || receipt.getStatus() == ReceiptStatus.REVISION_REQUIRED) {
            throw new BusinessRuleViolationException("RECEIPT_PENDING_MANAGER_APPROVAL");
        }
        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new BusinessRuleViolationException("RECEIPT_NOT_IN_DRAFT");
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

    private ReceiptQcResponse submitQc(Receipt receipt, List<ReceiptQcItemRequest> itemRequests, User actor) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("QC_ITEMS_REQUIRED");
        }

        QcSamplingMethod defaultMethod = defaultSamplingMethod(receipt);
        for (ReceiptQcItemRequest req : itemRequests) {
            ReceiptItem item = receiptItemRepository.findByIdAndReceiptId(req.getReceiptItemId(), receipt.getId())
                    .orElseThrow(() -> new IllegalArgumentException("RECEIPT_ITEM_NOT_FOUND: " + req.getReceiptItemId()));
            validateQcQuantities(item, req);

            item.setSampleQty(req.getSampleQty() != null ? req.getSampleQty() : item.getActualQty());
            item.setSamplePassedQty(req.getQcPassedQty());
            item.setSampleFailedQty(req.getQcFailedQty());
            item.setQualityPassedQty(req.getQualityPassedQty());
            item.setQualityFailedQty(req.getQualityFailedQty());
            item.setQuarantineReadyQty(0);
            item.setQcSamplingMethod(req.getQcSamplingMethod() != null ? req.getQcSamplingMethod() : defaultMethod);
            item.setQcFailureReason(req.getQcFailureReason());
            item.setQcBy(actor);
            item.setQcResult(req.getQualityFailedQty() > 0 ? QcResult.FAILED : QcResult.PASSED);
            receiptItemRepository.save(item);
        }

        auditLogService.log(actor, AuditAction.RECEIPT_QC_SUBMIT, "Receipt",
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null, Map.of("submittedItems", itemRequests.size()));

        return buildResponse(receipt);
    }

    private ReceiptQcResponse confirmQc(Receipt receipt, User actor) {
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receipt.getId());
        if (items.stream().anyMatch(i -> i.getQcResult() == null || i.getQcResult() == QcResult.PENDING)) {
            throw new BusinessRuleViolationException("QC_NOT_YET_SUBMITTED");
        }

        boolean anyFailed = items.stream().anyMatch(i -> safe(i.getQualityFailedQty()) > 0);
        ReceiptStatus resultStatus = anyFailed ? ReceiptStatus.QC_FAILED : ReceiptStatus.QC_COMPLETED;
        receipt.setStatus(resultStatus);
        receiptRepository.save(receipt);

        for (ReceiptItem item : items) {
            item.setQuarantineReadyQty(anyFailed ? safe(item.getQualityFailedQty()) : 0);
            receiptItemRepository.save(item);
        }

        auditLogService.log(actor, AuditAction.RECEIPT_QC_CONFIRM, "Receipt",
                receipt.getId(), receipt.getReceiptNumber(), receipt.getWarehouse().getId(),
                null, Map.of("result", resultStatus.name()));

        return buildResponse(receipt);
    }

    private void validateQcQuantities(ReceiptItem item, ReceiptQcItemRequest req) {
        Integer samplePassed = req.getQcPassedQty();
        Integer sampleFailed = req.getQcFailedQty();
        Integer sampleQty = req.getSampleQty() != null ? req.getSampleQty() : item.getActualQty();
        if (samplePassed == null || sampleFailed == null || sampleQty == null || samplePassed + sampleFailed != sampleQty) {
            throw new BusinessRuleViolationException("QC_SAMPLE_MISMATCH: item " + req.getReceiptItemId());
        }
        Integer qualityPassed = req.getQualityPassedQty();
        Integer qualityFailed = req.getQualityFailedQty();
        int actual = safe(item.getActualQty());
        if (qualityPassed == null || qualityFailed == null || qualityPassed + qualityFailed != actual) {
            throw new BusinessRuleViolationException("QC_QUANTITY_MISMATCH: item " + req.getReceiptItemId());
        }
        if (qualityFailed > 0 && (req.getQcFailureReason() == null || req.getQcFailureReason().isBlank())) {
            throw new BusinessRuleViolationException("QC_FAILED_REASON_REQUIRED: item " + req.getReceiptItemId());
        }
    }

    private QcSamplingMethod defaultSamplingMethod(Receipt receipt) {
        Long supplierId = receipt.getSupplier() != null ? receipt.getSupplier().getId() : null;
        long approvedCount = supplierId != null
                ? receiptItemRepository.countApprovedReceiptsBySupplierId(supplierId)
                : 0;
        return approvedCount >= TRUSTED_SUPPLIER_THRESHOLD
                ? QcSamplingMethod.RANDOM_SAMPLE
                : QcSamplingMethod.FULL_INSPECTION;
    }

    private ReceiptQcResponse buildResponse(Receipt receipt) {
        List<ReceiptItemQcResponse> itemResponses = receiptItemRepository.findByReceiptId(receipt.getId()).stream()
                .map(i -> ReceiptItemQcResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .productSku(i.getProduct().getSku())
                        .expectedQty(i.getExpectedQty())
                        .actualQty(i.getActualQty())
                        .sampleQty(i.getSampleQty())
                        .samplePassedQty(i.getSamplePassedQty())
                        .sampleFailedQty(i.getSampleFailedQty())
                        .qualityPassedQty(i.getQualityPassedQty())
                        .qualityFailedQty(i.getQualityFailedQty())
                        .quarantineReadyQty(i.getQuarantineReadyQty())
                        .qcSamplingMethod(i.getQcSamplingMethod())
                        .qcResult(i.getQcResult())
                        .qcFailureReason(i.getQcFailureReason())
                        .qcByUserId(i.getQcBy() != null ? i.getQcBy().getId() : null)
                        .build())
                .toList();

        return ReceiptQcResponse.builder()
                .receiptId(receipt.getId())
                .receiptNumber(receipt.getReceiptNumber())
                .status(receipt.getStatus())
                .items(itemResponses)
                .build();
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }
}
