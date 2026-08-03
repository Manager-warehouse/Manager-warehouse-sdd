package com.wms.repository;


import com.wms.entity.billing_payment.PaymentReceipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

    Optional<PaymentReceipt> findByPaymentNumber(String paymentNumber);

    List<PaymentReceipt> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    List<PaymentReceipt> findByInvoiceId(Long invoiceId);

    List<PaymentReceipt> findByAccountingPeriodIdOrderByCreatedAtDesc(Long accountingPeriodId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM PaymentReceipt p JOIN p.invoice i JOIN i.deliveryOrder do WHERE " +
           "do.warehouse.id IN :warehouseIds " +
           "AND (:dealerId IS NULL OR p.dealer.id = :dealerId) " +
           "AND (:periodId IS NULL OR p.accountingPeriod.id = :periodId) " +
           "ORDER BY p.createdAt DESC")
    List<PaymentReceipt> findFilteredPaymentReceiptsWithWarehouses(
            @org.springframework.data.repository.query.Param("warehouseIds") List<Long> warehouseIds,
            @org.springframework.data.repository.query.Param("dealerId") Long dealerId,
            @org.springframework.data.repository.query.Param("periodId") Long periodId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM PaymentReceipt p WHERE " +
           "(:dealerId IS NULL OR p.dealer.id = :dealerId) " +
           "AND (:periodId IS NULL OR p.accountingPeriod.id = :periodId) " +
           "ORDER BY p.createdAt DESC")
    List<PaymentReceipt> findFilteredPaymentReceiptsWithoutWarehouses(
            @org.springframework.data.repository.query.Param("dealerId") Long dealerId,
            @org.springframework.data.repository.query.Param("periodId") Long periodId);
}
