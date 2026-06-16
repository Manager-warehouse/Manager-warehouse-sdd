package com.wms.repository;

import com.wms.entity.Delivery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findFirstByDeliveryOrderIdOrderByCreatedAtDesc(Long doId);
}
