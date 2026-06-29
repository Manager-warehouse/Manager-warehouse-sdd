package com.wms.repository;

import com.wms.entity.DeliveryOrderItem;
import com.wms.enums.DeliveryOrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemRepository extends JpaRepository<DeliveryOrderItem, Long> {
    @EntityGraph(attributePaths = {"product", "batch", "location", "zone"})
    List<DeliveryOrderItem> findByDeliveryOrderId(Long deliveryOrderId);

    @EntityGraph(attributePaths = {"product", "batch", "location", "zone", "deliveryOrder"})
    @Query("""
            select i from DeliveryOrderItem i
            where i.deliveryOrder.id = :deliveryOrderId
              and i.deliveryOrder.status in :statuses
            """)
    List<DeliveryOrderItem> findDetailedByDeliveryOrderIdAndStatusIn(@Param("deliveryOrderId") Long deliveryOrderId,
                                                                     @Param("statuses") Collection<DeliveryOrderStatus> statuses);

    @EntityGraph(attributePaths = {"product", "batch", "location", "zone", "deliveryOrder"})
    @Query("""
            select i from DeliveryOrderItem i
            where i.deliveryOrder.id = :deliveryOrderId
              and i.qcPassQty > 0
            order by i.id asc
            """)
    List<DeliveryOrderItem> findItemsWithQcPassQtyByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);

    @EntityGraph(attributePaths = {"product", "batch", "location", "zone", "deliveryOrder", "deliveryOrder.warehouse"})
    @Query("""
            select i from DeliveryOrderItem i
            where i.deliveryOrder.id in :deliveryOrderIds
            order by i.deliveryOrder.id asc, i.id asc
            """)
    List<DeliveryOrderItem> findByDeliveryOrderIdIn(@Param("deliveryOrderIds") Collection<Long> deliveryOrderIds);

    @Query("""
            select i from DeliveryOrderItem i
            where i.deliveryOrder.status in (com.wms.enums.DeliveryOrderStatus.COMPLETED, com.wms.enums.DeliveryOrderStatus.CLOSED)
              and i.deliveryOrder.updatedAt >= :start
              and i.deliveryOrder.updatedAt <= :end
            """)
    List<DeliveryOrderItem> findCompletedItemsInPeriod(@Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);
}

