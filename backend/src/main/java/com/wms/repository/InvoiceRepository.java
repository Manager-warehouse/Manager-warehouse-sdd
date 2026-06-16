package com.wms.repository;

import com.wms.entity.Invoice;
import com.wms.enums.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByDeliveryOrderId(Long doId);

    List<Invoice> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    @Query("select i from Invoice i where i.dealer.id = :dealerId and i.status in :statuses order by i.dueDate asc")
    List<Invoice> findUnpaidInvoicesByDealer(
            @Param("dealerId") Long dealerId,
            @Param("statuses") List<InvoiceStatus> statuses);

    @Query("select count(i) > 0 from Invoice i where i.accountingPeriod.id = :periodId and i.status <> 'PAID'")
    boolean existsUnpaidInvoicesInPeriod(@Param("periodId") Long periodId);
}
