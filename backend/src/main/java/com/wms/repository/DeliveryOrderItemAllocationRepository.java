package com.wms.repository;

import com.wms.entity.DeliveryOrderItemAllocation;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = {
            "deliveryOrderItem", "deliveryOrderItem.product", "inventory",
            "batch", "location", "zone", "replacedAllocation"
    })
    @Query("""
            select a from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.deliveryOrder.id = :deliveryOrderId
              and a.pickedQty < a.plannedQty
            order by a.id asc
            """)
    List<DeliveryOrderItemAllocation> findActiveQcPendingAllocations(@Param("deliveryOrderId") Long deliveryOrderId);

    @Query("""
            select coalesce(sum(a.plannedQty), 0)
            from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.deliveryOrder.id = :deliveryOrderId
            """)
    BigDecimal sumPlannedQtyByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);
}
