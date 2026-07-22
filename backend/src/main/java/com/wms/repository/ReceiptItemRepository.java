package com.wms.repository;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.stock_receiving.ReceiptItem;
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

    @EntityGraph(attributePaths = { "product", "batch", "location" })
    List<ReceiptItem> findByReceiptId(Long receiptId);

    @EntityGraph(attributePaths = { "product", "batch", "location" })
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

    @Query("SELECT ri FROM ReceiptItem ri " +
           "JOIN FETCH ri.receipt r " +
           "JOIN FETCH ri.product p " +
           "WHERE r.warehouse.id = :warehouseId " +
           "  AND r.status = 'QC_FAILED' " +
           "  AND ri.sampleFailedQty > 0 " +
           "  AND NOT EXISTS ( " +
           "      SELECT 1 FROM Adjustment a " +
           "      WHERE a.referenceType = 'RECEIPT' " +
           "        AND a.referenceId = r.id " +
           "        AND a.type = 'RETURN_TO_VENDOR' " +
           "  )")
    List<ReceiptItem> findQuarantineItemsByWarehouseId(@Param("warehouseId") Long warehouseId);
}
