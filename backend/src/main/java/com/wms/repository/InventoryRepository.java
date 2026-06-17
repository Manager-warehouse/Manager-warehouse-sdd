package com.wms.repository;

import com.wms.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    interface AvailabilitySummary {
        BigDecimal getTotalQty();
        BigDecimal getReservedQty();
        BigDecimal getAvailableQty();
    }

    boolean existsByWarehouseIdAndTotalQtyGreaterThan(Long warehouseId, BigDecimal totalQty);

    boolean existsByLocationIdAndTotalQtyGreaterThan(Long locationId, BigDecimal totalQty);

    @Query("""
            select
                coalesce(sum(i.totalQty), 0) as totalQty,
                coalesce(sum(i.reservedQty), 0) as reservedQty,
                coalesce(sum(i.totalQty - i.reservedQty), 0) as availableQty
            from Inventory i
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
            """)
    AvailabilitySummary summarizeAvailability(@Param("warehouseId") Long warehouseId,
                                              @Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from Inventory i
            join fetch i.batch b
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
              and (i.totalQty - i.reservedQty) > 0
            order by b.receivedDate asc, i.id asc
            """)
    List<Inventory> findReservableForUpdate(@Param("warehouseId") Long warehouseId,
                                            @Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from Inventory i
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
              and i.batch.id = :batchId
              and i.location.id = :locationId
            """)
    Optional<Inventory> findByStockKeyForUpdate(@Param("warehouseId") Long warehouseId,
                                                @Param("productId") Long productId,
                                                @Param("batchId") Long batchId,
                                                @Param("locationId") Long locationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find inventory row for a specific warehouse/product/batch/location combo with a write lock.
     * Used during putaway and RTV confirm to prevent concurrent inventory corruption.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i " +
           "WHERE i.warehouse.id = :warehouseId " +
           "AND i.product.id = :productId " +
           "AND i.batch.id = :batchId " +
           "AND i.location.id = :locationId")
    Optional<Inventory> findByWarehouseProductBatchLocationForUpdate(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("batchId") Long batchId,
            @Param("locationId") Long locationId);

    /**
     * Find inventory row (read only) for a specific combo.
     */
    @Query("SELECT i FROM Inventory i " +
           "WHERE i.warehouse.id = :warehouseId " +
           "AND i.product.id = :productId " +
           "AND i.batch.id = :batchId " +
           "AND i.location.id = :locationId")
    Optional<Inventory> findByWarehouseProductBatchLocation(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("batchId") Long batchId,
            @Param("locationId") Long locationId);

    Optional<Inventory> findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
            Long warehouseId, Long productId, Long batchId, Long locationId);
}
