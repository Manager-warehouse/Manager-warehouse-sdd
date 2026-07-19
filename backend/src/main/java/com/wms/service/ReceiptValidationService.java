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
        if (actor == null) {
            throw new ForbiddenReceiptWarehouseException("FORBIDDEN_RECEIPT_ROLE: actor is null");
        }
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
        Long warehouseId = receipt.getWarehouse().getId();
        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository
                .findWarehouseIdsByUserId(actor.getId());
        if (!assignedWarehouseIds.contains(warehouseId)) {
            throw new ForbiddenReceiptWarehouseException(receiptId, warehouseId);
        }
    }

    @Transactional(readOnly = true)
    public void assertWarehouseAccess(User actor, Long warehouseId) {
        if (actor == null) {
            throw new ForbiddenReceiptWarehouseException("FORBIDDEN_RECEIPT_ROLE: actor is null");
        }
        // Accountants issue Credit Notes across warehouses and are never warehouse-assigned;
        // matches the same bypass already applied to ReceiptService.requireWarehouseAccess.
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO
                || actor.getRole() == UserRole.ACCOUNTANT || actor.getRole() == UserRole.ACCOUNTANT_MANAGER) {
            return;
        }
        List<Long> assignedWarehouseIds = userWarehouseAssignmentRepository
                .findWarehouseIdsByUserId(actor.getId());
        if (!assignedWarehouseIds.contains(warehouseId)) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_WAREHOUSE_ACCESS: User " + actor.getId()
                    + " is not assigned to warehouse " + warehouseId);
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
        if (actor == null) {
            throw new ForbiddenReceiptWarehouseException("FORBIDDEN_RECEIPT_ROLE: actor is null");
        }

        // ADMIN and CEO bypass for all actions
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }

        boolean isAuthorized = false;
        if (requiredRole == UserRole.WAREHOUSE_STAFF) {
            // STAFF tasks can also be done by STOREKEEPER and WAREHOUSE_MANAGER
            isAuthorized = actor.getRole() == UserRole.WAREHOUSE_STAFF
                    || actor.getRole() == UserRole.STOREKEEPER
                    || actor.getRole() == UserRole.WAREHOUSE_MANAGER;
        } else if (requiredRole == UserRole.STOREKEEPER) {
            // STOREKEEPER tasks can also be done by WAREHOUSE_MANAGER
            isAuthorized = actor.getRole() == UserRole.STOREKEEPER
                    || actor.getRole() == UserRole.WAREHOUSE_MANAGER;
        } else if (requiredRole == UserRole.WAREHOUSE_MANAGER) {
            isAuthorized = actor.getRole() == UserRole.WAREHOUSE_MANAGER;
        } else {
            isAuthorized = actor.getRole() == requiredRole;
        }

        if (!isAuthorized) {
            throw new ForbiddenReceiptWarehouseException(
                    "FORBIDDEN_RECEIPT_ROLE: " + action + " requires role " + requiredRole);
        }
    }
}
