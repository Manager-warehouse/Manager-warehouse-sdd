package com.wms.repository;

import com.wms.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByWarehouseIdAndTotalQtyGreaterThan(Long warehouseId, BigDecimal totalQty);

    boolean existsByLocationIdAndTotalQtyGreaterThan(Long locationId, BigDecimal totalQty);

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

    java.util.Optional<Inventory> findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
            Long warehouseId, Long productId, Long batchId, Long locationId);

    /**
     * Fetch all non-quarantine inventory rows with stock > 0 for a warehouse.
     * Used when populating stocktake items.
     */
    @Query("SELECT i FROM Inventory i " +
           "JOIN FETCH i.product " +
           "JOIN FETCH i.batch " +
           "JOIN FETCH i.location " +
           "WHERE i.warehouse.id = :warehouseId " +
           "AND i.location.isQuarantine = false " +
           "AND i.totalQty > 0")
    java.util.List<Inventory> findActiveNonQuarantineByWarehouseId(@Param("warehouseId") Long warehouseId);
}

