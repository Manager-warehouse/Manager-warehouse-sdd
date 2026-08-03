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
    List<SupplierInvoice> findByAccountingPeriodId(Long accountingPeriodId);

    // Catches double-entry of the same paper/PDF VAT invoice for a supplier - this row can
    // never be edited after creation, so catching a duplicate before it's saved is the only
    // recovery path.
    boolean existsBySupplierIdAndSupplierInvoiceNumber(Long supplierId, String supplierInvoiceNumber);
}
