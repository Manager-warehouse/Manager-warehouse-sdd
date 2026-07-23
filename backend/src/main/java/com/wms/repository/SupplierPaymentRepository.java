package com.wms.repository;

import com.wms.entity.billing_payment.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long>, JpaSpecificationExecutor<SupplierPayment> {
    Optional<SupplierPayment> findByPaymentNumber(String paymentNumber);
    List<SupplierPayment> findBySupplierId(Long supplierId);
    List<SupplierPayment> findBySupplierInvoiceId(Long supplierInvoiceId);
}
