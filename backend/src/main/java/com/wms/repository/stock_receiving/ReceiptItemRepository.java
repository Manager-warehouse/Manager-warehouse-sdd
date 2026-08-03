package com.wms.repository.stock_receiving;

import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @EntityGraph(attributePaths = { "product", "batch", "location" })
    List<ReceiptItem> findByReceiptId(Long receiptId);

    @EntityGraph(attributePaths = { "product", "batch", "location" })
    List<ReceiptItem> findByReceiptIdOrderByIdAsc(Long receiptId);

    Optional<ReceiptItem> findByIdAndReceiptId(Long id, Long receiptId);

    /**
     * Sum of actual quantities for items belonging to a receipt.
     */
    @Query("SELECT COALESCE(SUM(i.actualQty), 0) FROM ReceiptItem i WHERE i.receipt.id = :receiptId")
    BigDecimal sumActualQtyByReceiptId(@Param("receiptId") Long receiptId);

    /**
     * Find an item by receipt and product for batch resolution during approval.
     */
    Optional<ReceiptItem> findByReceiptIdAndProductId(Long receiptId, Long productId);

    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.supplier.id = :supplierId AND r.status = 'APPROVED'")
    long countApprovedReceiptsBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT ri FROM ReceiptItem ri " +
           "JOIN FETCH ri.receipt r " +
           "JOIN FETCH ri.product p " +
           "WHERE r.warehouse.id = :warehouseId " +
           "  AND (" +
           "    (ri.quarantineQty > ri.resolvedQuarantineQty " +
           "     AND r.status IN ('PARTIALLY_APPROVED', 'PUTAWAY_COMPLETED', 'RETURN_TO_SUPPLIER_PENDING') AND NOT EXISTS (" +
           "        SELECT 1 FROM Adjustment a " +
           "        WHERE a.referenceType = 'RECEIPT' " +
           "          AND a.referenceId = r.id " +
           "          AND a.type = 'RETURN_TO_VENDOR'" +
           "    )) OR " +
           "    (r.type = 'RETURN' AND r.status = 'APPROVED' AND ri.sampleFailedQty > 0 AND NOT EXISTS (" +
           "        SELECT 1 FROM Adjustment a " +
           "        WHERE a.referenceType = 'RECEIPT_ITEM' " +
           "          AND a.referenceId = ri.id " +
           "          AND a.type = 'DISPOSAL'" +
           "    ))" +
           "  )")
    List<ReceiptItem> findQuarantineItemsByWarehouseId(@Param("warehouseId") Long warehouseId);

    Optional<ReceiptItem> findFirstByBatchId(Long batchId);

    @Query("SELECT ri.receipt.supplier.id FROM ReceiptItem ri WHERE ri.id = :receiptItemId AND ri.receipt.supplier IS NOT NULL")
    Optional<Long> findSupplierIdByReceiptItemId(@Param("receiptItemId") Long receiptItemId);

    @Query("SELECT ri.receipt.supplier.companyName FROM ReceiptItem ri WHERE ri.id = :receiptItemId AND ri.receipt.supplier IS NOT NULL")
    Optional<String> findSupplierNameByReceiptItemId(@Param("receiptItemId") Long receiptItemId);

    @Query("SELECT ri.receipt.supplier.id FROM ReceiptItem ri WHERE ri.batch.id = :batchId AND ri.receipt.supplier IS NOT NULL")
    Optional<Long> findSupplierIdByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT ri.receipt.supplier.companyName FROM ReceiptItem ri WHERE ri.batch.id = :batchId AND ri.receipt.supplier IS NOT NULL")
    Optional<String> findSupplierNameByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT ri.receipt.supplier.id FROM ReceiptItem ri WHERE ri.product.id = :productId AND ri.receipt.supplier IS NOT NULL ORDER BY ri.id DESC")
    List<Long> findSupplierIdsByProductId(@Param("productId") Long productId);

    @Query("SELECT ri.receipt.supplier.companyName FROM ReceiptItem ri WHERE ri.product.id = :productId AND ri.receipt.supplier IS NOT NULL ORDER BY ri.id DESC")
    List<String> findSupplierNamesByProductId(@Param("productId") Long productId);
}
