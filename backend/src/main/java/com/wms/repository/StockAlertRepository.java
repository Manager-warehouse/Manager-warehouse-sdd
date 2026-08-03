package com.wms.repository;


import com.wms.entity.notification_delivery.StockAlert;
import com.wms.enums.notification_delivery.AlertType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Optional<StockAlert> findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(
            Long warehouseId, Long productId, AlertType alertType, Boolean isResolved);

    @Modifying
    @Query(value = """
            INSERT INTO stock_alerts
                (warehouse_id, product_id, current_qty, reorder_point, alert_type, is_resolved, created_at)
            VALUES
                (:warehouseId, :productId, :currentQty, :reorderPoint, :alertType, false, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertOpenAlertIfAbsent(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("currentQty") BigDecimal currentQty,
            @Param("reorderPoint") BigDecimal reorderPoint,
            @Param("alertType") String alertType);

    @Query("""
            select sa from StockAlert sa
            where (:warehouseId is null or sa.warehouse.id = :warehouseId)
              and (:productId is null or sa.product.id = :productId)
              and (:isResolved is null or sa.isResolved = :isResolved)
            """)
    Page<StockAlert> findWithFilters(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("isResolved") Boolean isResolved,
            Pageable pageable);

    long countByWarehouseIdAndIsResolvedFalse(Long warehouseId);
}
