package com.wms.repository;

import com.wms.entity.billing_payment.SupplierBillingNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierBillingNotificationRepository extends JpaRepository<SupplierBillingNotification, Long> {
    Optional<SupplierBillingNotification> findByReceiptId(Long receiptId);
    List<SupplierBillingNotification> findByStatusAndInvoiceStatus(String status, String invoiceStatus);
    List<SupplierBillingNotification> findBySupplierId(Long supplierId);
}
