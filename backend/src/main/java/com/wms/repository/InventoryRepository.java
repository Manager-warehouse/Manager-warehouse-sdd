package com.wms.repository;

import com.wms.entity.Inventory;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
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
                          and i.location.isActive = true
                          and i.location.isQuarantine = false
                          and i.location.isLocked = false
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
         * Find inventory row for a specific warehouse/product/batch/location combo with
         * a write lock.
         * Used during putaway and RTV confirm to prevent concurrent inventory
         * corruption.
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

        @EntityGraph(attributePaths = { "warehouse", "product", "batch", "location", "location.parent" })
        @Query("""
                        select i from Inventory i
                        where i.warehouse.id = :warehouseId
                          and i.product.id = :productId
                          and i.warehouse.type <> com.wms.enums.WarehouseType.IN_TRANSIT
                          and i.location.type = com.wms.enums.LocationType.BIN
                          and i.location.isActive = true
                          and i.location.isLocked = false
                          and i.location.isQuarantine = false
                          and i.totalQty > i.reservedQty
                        order by i.batch.receivedDate asc, i.id asc
                        """)
        List<Inventory> findValidFifoCandidates(@Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId);

        @EntityGraph(attributePaths = { "warehouse", "product", "batch", "location", "location.parent" })
        @Query("""
                        select i from Inventory i
                        where i.warehouse.id = :warehouseId
                          and i.product.id = :productId
                          and i.warehouse.type <> com.wms.enums.WarehouseType.IN_TRANSIT
                          and i.location.type = com.wms.enums.LocationType.BIN
                          and i.location.isActive = true
                          and i.location.isLocked = false
                          and i.location.isQuarantine = false
                          and i.totalQty > 0
                        order by i.batch.receivedDate asc, i.id asc
                        """)
        List<Inventory> findFifoRowsForPlanning(@Param("warehouseId") Long warehouseId,
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

        @Lock(LockModeType.OPTIMISTIC)
        @Query("""
                        select i from Inventory i
                        where i.warehouse.id = :warehouseId
                          and i.product.id = :productId
                          and i.batch.id = :batchId
                          and i.location.id = :locationId
                        """)
        Optional<Inventory> findConcreteReservationRowForUpdate(@Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId,
                        @Param("batchId") Long batchId,
                        @Param("locationId") Long locationId);

        @Lock(LockModeType.OPTIMISTIC)
        @Query("""
                        select i from Inventory i
                        where i.id in :ids
                        """)
        List<Inventory> findByIdInWithLock(@Param("ids") List<Long> ids);

        @EntityGraph(attributePaths = { "warehouse", "product", "batch", "location", "location.parent" })
        List<Inventory> findByIdIn(List<Long> ids);

        @EntityGraph(attributePaths = { "warehouse", "product", "batch", "location", "location.parent" })
        Optional<Inventory> findWithDetailsById(Long id);

        @Lock(LockModeType.OPTIMISTIC)
        @Query("""
                        select i from Inventory i
                        where i.warehouse.id = :warehouseId
                          and i.product.id = :productId
                          and i.batch.id = :batchId
                          and i.location.id = :locationId
                        """)
        Optional<Inventory> findConcreteRowForTripMovement(@Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId,
                        @Param("batchId") Long batchId,
                        @Param("locationId") Long locationId);

        @Lock(LockModeType.OPTIMISTIC)
        @Query("""
                        select i from Inventory i
                        where i.warehouse.type = com.wms.enums.WarehouseType.IN_TRANSIT
                          and i.product.id = :productId
                          and i.batch.id = :batchId
                          and i.totalQty > 0
                        order by i.id asc
                        limit 1
                        """)
        Optional<Inventory> findTransitRowForDeliveryConfirmation(@Param("productId") Long productId,
                        @Param("batchId") Long batchId);
}
