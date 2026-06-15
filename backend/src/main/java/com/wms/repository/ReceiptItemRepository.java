package com.wms.repository;

import com.wms.entity.ReceiptItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @EntityGraph(attributePaths = {"product", "batch", "location"})
    List<ReceiptItem> findByReceiptId(Long receiptId);

    @EntityGraph(attributePaths = {"product", "batch", "location"})
    List<ReceiptItem> findByReceiptIdOrderByIdAsc(Long receiptId);

    Optional<ReceiptItem> findByIdAndReceiptId(Long id, Long receiptId);

    /**
     * Sum of actual quantities for items belonging to a receipt.
     * Used to compute total quarantine quantity for RTV validation.
     */
    @Query("SELECT COALESCE(SUM(i.actualQty), 0) FROM ReceiptItem i WHERE i.receipt.id = :receiptId")
    BigDecimal sumActualQtyByReceiptId(@Param("receiptId") Long receiptId);

    /**
     * Find an item by receipt and product for batch resolution during approval.
     */
    Optional<ReceiptItem> findByReceiptIdAndProductId(Long receiptId, Long productId);

    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.supplier.id = :supplierId AND r.status = 'APPROVED'")
    long countApprovedReceiptsBySupplierId(@Param("supplierId") Long supplierId);
}
