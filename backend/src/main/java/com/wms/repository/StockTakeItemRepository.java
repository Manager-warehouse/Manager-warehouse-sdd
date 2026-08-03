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
import com.wms.entity.stock_counting.StockTakeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho dòng hàng kiểm kê (stock_take_items).
 * Dùng bởi: StockTakeService
 */
@Repository
public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

    /** Load items kèm JOIN FETCH product, batch, location. Dùng bởi: buildResponse, executeApproval */
    @Query("SELECT i FROM StockTakeItem i " +
           "JOIN FETCH i.product " +
           "JOIN FETCH i.batch " +
           "JOIN FETCH i.location " +
           "WHERE i.stockTake.id = :stockTakeId")
    List<StockTakeItem> findByStockTakeIdWithDetails(@Param("stockTakeId") Long stockTakeId);

    /** Load items cơ bản (lazy). Dùng bởi: completeStockTake (refresh systemQty), lockLocations */
    List<StockTakeItem> findByStockTakeId(Long stockTakeId);

    /** Tìm item theo tổ hợp unique. Hiện chưa dùng trong service nhưng hỗ trợ lookup nếu cần */
    Optional<StockTakeItem> findByStockTakeIdAndProductIdAndBatchIdAndLocationId(
            Long stockTakeId, Long productId, Long batchId, Long locationId);

    /** Kiểm tra còn item nào chưa đếm (actual_qty = null). Dùng bởi: completeStockTake — chặn hoàn tất nếu chưa đủ */
    boolean existsByStockTakeIdAndActualQtyIsNull(Long stockTakeId);
}
