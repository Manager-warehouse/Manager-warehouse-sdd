package com.wms.repository;

import com.wms.entity.PaymentReceipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

    Optional<PaymentReceipt> findByPaymentNumber(String paymentNumber);

    List<PaymentReceipt> findByDealerIdOrderByCreatedAtDesc(Long dealerId);

    List<PaymentReceipt> findByAccountingPeriodIdOrderByCreatedAtDesc(Long accountingPeriodId);
}
