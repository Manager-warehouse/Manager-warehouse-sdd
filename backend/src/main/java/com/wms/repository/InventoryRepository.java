package com.wms.repository;

import com.wms.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByWarehouseIdAndTotalQtyGreaterThan(Long warehouseId, BigDecimal totalQty);

    boolean existsByLocationIdAndTotalQtyGreaterThan(Long locationId, BigDecimal totalQty);

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Inventory> findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
            Long warehouseId, Long productId, Long batchId, Long locationId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :wId AND i.product.id = :pId " +
           "AND i.batch IS NULL AND i.location.id = :lId")
    Optional<Inventory> findQuarantineInventory(
            @Param("wId") Long warehouseId,
            @Param("pId") Long productId,
            @Param("lId") Long locationId);

    @Query("SELECT i FROM Inventory i WHERE i.location.id = :locationId")
    List<Inventory> findByLocationId(@Param("locationId") Long locationId);
}
