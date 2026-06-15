package com.wms.service;

import com.wms.entity.Receipt;
import com.wms.entity.User;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Shared validation and authorization checks for inbound receipts.
 */
@Service
public class ReceiptValidationService {

    private final ReceiptRepository receiptRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    public ReceiptValidationService(ReceiptRepository receiptRepository,
                                    UserWarehouseAssignmentRepository userWarehouseAssignmentRepository) {
        this.receiptRepository = receiptRepository;
        this.userWarehouseAssignmentRepository = userWarehouseAssignmentRepository;
    }

    @Transactional(readOnly = true)
    public Receipt loadReceiptForUpdate(Long receiptId) {
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
    }

    @Transactional(readOnly = true)
    public void assertWarehouseAssignment(User actor, Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
        Long warehouseId = receipt.getWarehouse().getId();
        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository
                .findWarehouseIdsByUserId(actor.getId());
        if (!assignedWarehouseIds.contains(warehouseId)) {
            throw new ForbiddenReceiptWarehouseException(receiptId, warehouseId);
        }
    }

    public void assertVersionMatch(Receipt receipt, Integer expectedVersion) {
        if (!receipt.getVersion().equals(expectedVersion)) {
            throw new BusinessRuleViolationException(
                    "INVENTORY_VERSION_CONFLICT: Receipt " + receipt.getId()
                    + " has been modified since you last loaded it (expected version "
                    + expectedVersion + ", current version " + receipt.getVersion()
                    + "). Please reload and retry.");
        }
    }

    public void assertRole(User actor, UserRole requiredRole, String action) {
        if (actor == null || actor.getRole() != requiredRole) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_RECEIPT_ROLE: " + action + " requires role " + requiredRole);
        }
    }
}
