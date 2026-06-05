package com.wms.repository;

import com.wms.entity.DeliveryOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItem, Long> {
    @EntityGraph(attributePaths = {"product", "batch", "location"})
    List<DeliveryOrderItem> findByDeliveryOrderId(Long deliveryOrderId);
}
