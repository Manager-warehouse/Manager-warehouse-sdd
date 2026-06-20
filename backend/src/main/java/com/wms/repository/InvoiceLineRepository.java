package com.wms.repository;

import com.wms.entity.InvoiceLine;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {

    @EntityGraph(attributePaths = {"deliveryOrderItem", "product"})
    List<InvoiceLine> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
