package com.wms.repository;

import com.wms.entity.order_fulfillment.SplitDeliveryPlan;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SplitDeliveryPlanRepository extends JpaRepository<SplitDeliveryPlan, Long> {

    boolean existsByPlanNumber(String planNumber);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrder.warehouse", "warehouse", "dispatcher",
            "leadDriver", "leadDriver.user"
    })
    @Query("select p from SplitDeliveryPlan p where p.id = :id")
    Optional<SplitDeliveryPlan> findDetailedById(@Param("id") Long id);

    @Query("""
            select count(p) > 0
            from SplitDeliveryPlan p
            where p.deliveryOrder.id = :deliveryOrderId
              and p.status in :statuses
              and (:excludedPlanId is null or p.id <> :excludedPlanId)
            """)
    boolean existsActivePlanForDeliveryOrder(@Param("deliveryOrderId") Long deliveryOrderId,
                                             @Param("statuses") Collection<SplitDeliveryPlanStatus> statuses,
                                             @Param("excludedPlanId") Long excludedPlanId);
}
