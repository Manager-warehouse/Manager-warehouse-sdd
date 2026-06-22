package com.wms.repository;

import com.wms.entity.WarehouseProductReservation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseProductReservationRepository extends JpaRepository<WarehouseProductReservation, Long> {

  Optional<WarehouseProductReservation> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

  @EntityGraph(attributePaths = { "warehouse", "product" })
  @Query("""
      select r from WarehouseProductReservation r
      where r.warehouse.id = :warehouseId
        and r.product.id = :productId
      """)
  Optional<WarehouseProductReservation> findWithWarehouseAndProductByWarehouseIdAndProductId(
      @Param("warehouseId") Long warehouseId,
      @Param("productId") Long productId);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("""
      select r from WarehouseProductReservation r
      where r.warehouse.id = :warehouseId
        and r.product.id = :productId
      """)
  Optional<WarehouseProductReservation> findWithWarehouseAndProductByWarehouseIdAndProductIdForUpdate(
      @Param("warehouseId") Long warehouseId,
      @Param("productId") Long productId);
}
