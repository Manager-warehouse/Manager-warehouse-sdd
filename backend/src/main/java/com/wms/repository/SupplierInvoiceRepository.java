package com.wms.repository;

import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.enums.billing_payment.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierInvoiceRepository extends JpaRepository<SupplierInvoice, Long>, JpaSpecificationExecutor<SupplierInvoice> {
    Optional<SupplierInvoice> findByInvoiceNumber(String invoiceNumber);
    Optional<SupplierInvoice> findByReceiptId(Long receiptId);
    List<SupplierInvoice> findBySupplierId(Long supplierId);
    List<SupplierInvoice> findBySupplierIdAndStatus(Long supplierId, InvoiceStatus status);
}
