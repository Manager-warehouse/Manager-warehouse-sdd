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
import com.wms.entity.stock_counting.StockTake;
import com.wms.enums.stock_counting.StockTakeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho phiếu kiểm kê (stock_takes).
 * Dùng bởi: StockTakeService
 */
@Repository
public interface StockTakeRepository extends JpaRepository<StockTake, Long> {

    /** Danh sách phiếu theo kho, sắp xếp mới nhất. Dùng bởi: getStockTakes (không phân trang) */
    List<StockTake> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    /** Danh sách phiếu theo kho + status. Dùng bởi: getStockTakes (không phân trang, có lọc status) */
    List<StockTake> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, StockTakeStatus status);

    /** Danh sách phiếu theo kho, có phân trang. Dùng bởi: getStockTakes (phân trang) */
    Page<StockTake> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId, Pageable pageable);

    /** Danh sách phiếu theo kho + status, có phân trang. Dùng bởi: getStockTakes (phân trang + lọc) */
    Page<StockTake> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, StockTakeStatus status, Pageable pageable);

    /** Load phiếu kèm JOIN FETCH warehouse, conductedBy, accountingPeriod. Dùng bởi: getStockTakeById */
    @Query("SELECT st FROM StockTake st " +
           "LEFT JOIN FETCH st.warehouse " +
           "LEFT JOIN FETCH st.conductedBy " +
           "LEFT JOIN FETCH st.accountingPeriod " +
           "WHERE st.id = :id")
    Optional<StockTake> findByIdWithDetails(@Param("id") Long id);

    /** Load phiếu với PESSIMISTIC_WRITE lock — ngăn concurrent modification. Dùng bởi: approve, reject, start, count, complete, cancel */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTake st WHERE st.id = :id")
    Optional<StockTake> findByIdForUpdate(@Param("id") Long id);

    /** Kiểm tra có phiếu đang hoạt động (DRAFT/IN_PROGRESS/PENDING_APPROVAL) trong kho. Dùng bởi: createStockTake — ngăn trùng phiếu */
    boolean existsByWarehouseIdAndStatusIn(Long warehouseId, List<StockTakeStatus> statuses);
}
