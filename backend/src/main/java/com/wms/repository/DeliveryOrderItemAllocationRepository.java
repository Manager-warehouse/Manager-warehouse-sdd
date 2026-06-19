package com.wms.repository;

import com.wms.entity.DeliveryOrderItemAllocation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemAllocationRepository extends JpaRepository<DeliveryOrderItemAllocation, Long> {

    @EntityGraph(attributePaths = {
            "deliveryOrderItem", "deliveryOrderItem.product", "inventory",
            "batch", "location", "zone", "replacedAllocation"
    })
    List<DeliveryOrderItemAllocation> findByDeliveryOrderItemDeliveryOrderId(Long deliveryOrderId);

    @EntityGraph(attributePaths = {
            "deliveryOrderItem", "deliveryOrderItem.product", "inventory",
            "batch", "location", "zone", "replacedAllocation"
    })
    List<DeliveryOrderItemAllocation> findByDeliveryOrderItemIdIn(Collection<Long> deliveryOrderItemIds);
}
