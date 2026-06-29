package com.wms.repository;

import com.wms.entity.StockAlert;
import com.wms.enums.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Optional<StockAlert> findByWarehouseIdAndProductIdAndAlertTypeAndIsResolved(
            Long warehouseId, Long productId, AlertType alertType, Boolean isResolved);

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
}
