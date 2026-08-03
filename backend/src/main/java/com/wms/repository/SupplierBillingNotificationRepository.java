package com.wms.repository;

import com.wms.entity.billing_payment.SupplierBillingNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierBillingNotificationRepository extends JpaRepository<SupplierBillingNotification, Long> {
    Optional<SupplierBillingNotification> findByReceiptId(Long receiptId);
    List<SupplierBillingNotification> findByStatusAndInvoiceStatus(String status, String invoiceStatus);
    List<SupplierBillingNotification> findBySupplierId(Long supplierId);

    // Only return notifications whose receipt is genuinely PUTAWAY_COMPLETED,
    // guarding against stale notifications left over when a receipt status was
    // reset during development or a rare rollback scenario.
    @Query("SELECT n FROM SupplierBillingNotification n JOIN FETCH n.receipt r "
            + "WHERE n.status = :status AND n.invoiceStatus = :invoiceStatus "
            + "AND r.status = com.wms.enums.stock_receiving.ReceiptStatus.PUTAWAY_COMPLETED")
    List<SupplierBillingNotification> findActiveNotInvoicedWithPutawayCompleted(
            String status, String invoiceStatus);
}
