package com.wms.repository;

import com.wms.entity.Invoice;
import com.wms.enums.InvoiceStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByDealerIdAndStatusInAndDueDateBefore(Long dealerId,
                                                        Iterable<InvoiceStatus> statuses,
                                                        LocalDate dueDate);

    boolean existsByDeliveryOrderId(Long deliveryOrderId);

    Optional<Invoice> findByDeliveryOrderId(Long deliveryOrderId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    java.util.List<Invoice> findByIssueDateBetween(LocalDate start, LocalDate end);

    java.util.List<Invoice> findByStatusNotAndDueDateBefore(InvoiceStatus status, LocalDate date);
}

