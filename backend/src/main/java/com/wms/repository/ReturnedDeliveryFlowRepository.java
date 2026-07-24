package com.wms.repository;

import com.wms.entity.order_fulfillment.ReturnedDeliveryFlow;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnedDeliveryFlowRepository extends JpaRepository<ReturnedDeliveryFlow, Long> {

    boolean existsByDeliveryOrderId(Long deliveryOrderId);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrder.warehouse", "items", "items.deliveryOrderItem",
            "items.product", "items.batch", "items.destinationLocation"
    })
    Optional<ReturnedDeliveryFlow> findByDeliveryOrderId(Long deliveryOrderId);
}
