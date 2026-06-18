package com.wms.repository;

import com.wms.entity.Inventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsByWarehouseIdAndTotalQtyGreaterThan(Long warehouseId, BigDecimal totalQty);
    boolean existsByLocationIdAndTotalQtyGreaterThan(Long locationId, BigDecimal totalQty);

    @Query("""
            select coalesce(sum(i.totalQty - i.reservedQty), 0)
            from Inventory i
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
              and i.location.isActive = true
              and i.location.isQuarantine = false
              and i.totalQty > i.reservedQty
            """)
    BigDecimal sumValidAvailableQty(@Param("warehouseId") Long warehouseId,
                                    @Param("productId") Long productId);

    @Query("""
            select i from Inventory i
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
              and i.batch.id = :batchId
              and i.location.id = :locationId
            """)
    List<Inventory> findConcreteReservationRows(@Param("warehouseId") Long warehouseId,
                                                @Param("productId") Long productId,
                                                @Param("batchId") Long batchId,
                                                @Param("locationId") Long locationId);
}
