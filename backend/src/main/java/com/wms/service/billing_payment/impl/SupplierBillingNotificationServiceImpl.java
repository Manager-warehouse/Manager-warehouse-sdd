package com.wms.service.billing_payment.impl;

import com.wms.dto.response.SupplierBillingNotificationResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.SupplierBillingNotification;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.SupplierBillingNotificationRepository;
import com.wms.service.billing_payment.SupplierBillingNotificationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SupplierBillingNotificationServiceImpl implements SupplierBillingNotificationService {

    private final SupplierBillingNotificationRepository supplierBillingNotificationRepository;

    public SupplierBillingNotificationServiceImpl(SupplierBillingNotificationRepository supplierBillingNotificationRepository) {
        this.supplierBillingNotificationRepository = supplierBillingNotificationRepository;
    }

    @Override
    @Transactional
    public void createNotificationForReceiptOrder(Receipt receipt) {
        if (receipt.getSupplier() == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        SupplierBillingNotification notification = SupplierBillingNotification.builder()
                .receipt(receipt)
                .receiptNumber(receipt.getReceiptNumber())
                .supplier(receipt.getSupplier())
                .supplierName(receipt.getSupplier().getCompanyName())
                .warehouse(receipt.getWarehouse())
                .completedAt(now)
                .totalAmountEstimate(BigDecimal.ZERO)
                .invoiceStatus("NOT_INVOICED")
                .status("ACTIVE")
                .recipientRole("ACCOUNTANT")
                .createdAt(now)
                .build();

        supplierBillingNotificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierBillingNotificationResponse> getPendingNotifications(User actor) {
        requireAccountant(actor);
        List<SupplierBillingNotification> list = supplierBillingNotificationRepository
                .findByStatusAndInvoiceStatus("ACTIVE", "NOT_INVOICED");
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long id, User actor) {
        requireAccountant(actor);
        SupplierBillingNotification notification = supplierBillingNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setReadAt(OffsetDateTime.now());
        notification.setStatus("READ");
        supplierBillingNotificationRepository.save(notification);
    }

    private void requireAccountant(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied: Accountant privileges required");
        }
    }

    private SupplierBillingNotificationResponse toResponse(SupplierBillingNotification entity) {
        return SupplierBillingNotificationResponse.builder()
                .id(entity.getId())
                .receiptId(entity.getReceipt().getId())
                .receiptNumber(entity.getReceiptNumber())
                .supplierId(entity.getSupplier().getId())
                .supplierName(entity.getSupplierName())
                .warehouseId(entity.getWarehouse().getId())
                .completedAt(entity.getCompletedAt())
                .totalAmountEstimate(entity.getTotalAmountEstimate())
                .invoiceStatus(entity.getInvoiceStatus())
                .status(entity.getStatus())
                .recipientRole(entity.getRecipientRole())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
