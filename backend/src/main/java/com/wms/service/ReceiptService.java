package com.wms.service;

import com.wms.entity.Receipt;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.enums.ReceiptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service demonstrating audit logging integration pattern.
 */
@Service
public class ReceiptService {

    private final AuditLogService auditLogService;

    public ReceiptService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Example: Creating a new inbound receipt.
     */
    @Transactional
    public Receipt createReceipt(Receipt receipt, User creator) {
        // 1. Business logic to persist receipt
        // receiptRepository.save(receipt); (omitted for demo)

        // 2. Prepare oldValue (null for CREATE) and newValue maps
        Map<String, Object> newValue = Map.of(
                "receiptNumber", receipt.getReceiptNumber(),
                "status", ReceiptStatus.PENDING_RECEIPT.name(),
                "supplierId", receipt.getSupplier() != null ? receipt.getSupplier().getId() : "null"
        );

        // 3. Log the operation
        auditLogService.log(
                creator,
                AuditAction.CREATE,
                "RECEIPT",
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null,
                null,
                newValue
        );

        return receipt;
    }

    /**
     * Example: Approving an inbound receipt.
     */
    @Transactional
    public void approveReceipt(Receipt receipt, User approver) {
        ReceiptStatus oldStatus = receipt.getStatus();
        
        // Update state
        receipt.setStatus(ReceiptStatus.APPROVED);
        // receiptRepository.save(receipt); (omitted for demo)

        // Log the change (diff-only)
        Map<String, Object> oldValue = Map.of("status", oldStatus.name());
        Map<String, Object> newValue = Map.of("status", ReceiptStatus.APPROVED.name());

        auditLogService.log(
                approver,
                AuditAction.APPROVE,
                "RECEIPT",
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null,
                oldValue,
                newValue
        );
    }
}
