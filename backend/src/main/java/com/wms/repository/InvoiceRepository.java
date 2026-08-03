package com.wms.repository;


import com.wms.entity.billing_payment.Invoice;
import com.wms.enums.billing_payment.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    List<Invoice> findByStatusOrderByCreatedAtDesc(InvoiceStatus status);

    List<Invoice> findByAccountingPeriodId(Long accountingPeriodId);

    @Query("select i from Invoice i where i.dealer.id = :dealerId and i.status in :statuses order by i.dueDate asc")
    List<Invoice> findUnpaidInvoicesByDealer(
            @Param("dealerId") Long dealerId,
            @Param("statuses") List<InvoiceStatus> statuses);

    @Query("SELECT i FROM Invoice i WHERE " +
           "i.deliveryOrder.warehouse.id IN :warehouseIds " +
           "AND (:dealerId IS NULL OR i.dealer.id = :dealerId) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findFilteredInvoicesWithWarehouses(
            @Param("warehouseIds") List<Long> warehouseIds,
            @Param("dealerId") Long dealerId,
            @Param("status") InvoiceStatus status);

    @Query("SELECT i FROM Invoice i WHERE " +
           "(:dealerId IS NULL OR i.dealer.id = :dealerId) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findFilteredInvoicesWithoutWarehouses(
            @Param("dealerId") Long dealerId,
            @Param("status") InvoiceStatus status);
}
