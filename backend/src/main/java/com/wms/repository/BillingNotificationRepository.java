package com.wms.repository;

import com.wms.entity.BillingNotification;
import com.wms.enums.BillingNotificationInvoiceStatus;
import com.wms.enums.BillingNotificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingNotificationRepository extends JpaRepository<BillingNotification, Long> {

    @EntityGraph(attributePaths = {"deliveryOrder", "dealer", "warehouse"})
    Optional<BillingNotification> findByDeliveryOrderIdAndInvoiceStatusAndStatus(
            Long deliveryOrderId,
            BillingNotificationInvoiceStatus invoiceStatus,
            BillingNotificationStatus status);

    @EntityGraph(attributePaths = {"deliveryOrder", "dealer", "warehouse"})
    List<BillingNotification> findByStatusOrderByCreatedAtDesc(BillingNotificationStatus status);

    @EntityGraph(attributePaths = {"deliveryOrder", "dealer", "warehouse"})
    List<BillingNotification> findByInvoiceStatusOrderByCreatedAtDesc(
            BillingNotificationInvoiceStatus invoiceStatus);
}
