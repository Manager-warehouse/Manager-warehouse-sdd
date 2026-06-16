package com.wms.repository;

import com.wms.entity.WarehouseProductReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseProductReservationRepository extends JpaRepository<WarehouseProductReservation, Long> {

    Optional<WarehouseProductReservation> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
}
