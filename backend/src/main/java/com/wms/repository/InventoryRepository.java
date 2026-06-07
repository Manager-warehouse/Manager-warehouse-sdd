package com.wms.repository;

import com.wms.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsByWarehouseIdAndTotalQtyGreaterThan(Long warehouseId, BigDecimal totalQty);
    boolean existsByLocationIdAndTotalQtyGreaterThan(Long locationId, BigDecimal totalQty);
}
