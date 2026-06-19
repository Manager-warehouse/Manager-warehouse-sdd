package com.wms.repository;

import com.wms.entity.Inventory;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
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

    @EntityGraph(attributePaths = {"warehouse", "product", "batch", "location", "location.parent"})
    @Query("""
            select i from Inventory i
            where i.warehouse.id = :warehouseId
              and i.product.id = :productId
              and i.warehouse.type <> com.wms.enums.WarehouseType.IN_TRANSIT
              and i.location.isActive = true
              and i.location.isQuarantine = false
              and i.totalQty > i.reservedQty
            order by i.batch.receivedDate asc, i.id asc
            """)
    List<Inventory> findValidFifoCandidates(@Param("warehouseId") Long warehouseId,
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

    @EntityGraph(attributePaths = {"warehouse", "product", "batch", "location", "location.parent"})
    @Lock(LockModeType.OPTIMISTIC)
    @Query("""
            select i from Inventory i
            where i.id in :ids
            """)
    List<Inventory> findByIdInWithLock(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"warehouse", "product", "batch", "location", "location.parent"})
    List<Inventory> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"warehouse", "product", "batch", "location", "location.parent"})
    Optional<Inventory> findWithDetailsById(Long id);
}
