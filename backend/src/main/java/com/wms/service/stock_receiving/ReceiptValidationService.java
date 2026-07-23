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
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.access_control.User;
import com.wms.enums.access_control.UserRole;
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

    // Must run inside the caller's write transaction (not readOnly) — a
    // PESSIMISTIC_WRITE lock acquired here is only held for the duration of
    // the enclosing transaction, and a read-only one would defeat the point:
    // two concurrent approve/reject calls on the same receipt could both
    // load the pre-mutation state and race past each other before either
    // commits, previously undetected until the version check at save time.
    @Transactional
    public Receipt loadReceiptForUpdate(Long receiptId) {
        return receiptRepository.findByIdForUpdate(receiptId)
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
