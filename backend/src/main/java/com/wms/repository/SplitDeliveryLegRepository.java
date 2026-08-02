package com.wms.repository;

import com.wms.entity.order_fulfillment.SplitDeliveryLeg;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SplitDeliveryLegRepository extends JpaRepository<SplitDeliveryLeg, Long> {

    @EntityGraph(attributePaths = {"splitPlan", "trip", "vehicle", "driver", "driver.user"})
    List<SplitDeliveryLeg> findBySplitPlanIdOrderByStopOrderAsc(Long splitPlanId);

    @EntityGraph(attributePaths = {"splitPlan", "trip", "vehicle", "driver", "driver.user"})
    @Query("""
            select leg from SplitDeliveryLeg leg
            where leg.splitPlan.id = :splitPlanId
              and leg.driver.user.id = :userId
            """)
    Optional<SplitDeliveryLeg> findBySplitPlanIdAndDriverUserId(@Param("splitPlanId") Long splitPlanId,
                                                                @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"splitPlan", "splitPlan.deliveryOrder", "trip", "vehicle", "driver", "driver.user"})
    Optional<SplitDeliveryLeg> findByTripId(Long tripId);

    @Query("""
            select count(leg) > 0
            from SplitDeliveryLeg leg
            where leg.vehicle.id = :vehicleId
              and leg.status in :statuses
              and (:excludedPlanId is null or leg.splitPlan.id <> :excludedPlanId)
            """)
    boolean existsActiveVehicleLeg(@Param("vehicleId") Long vehicleId,
                                   @Param("statuses") Collection<SplitDeliveryPlanStatus> statuses,
                                   @Param("excludedPlanId") Long excludedPlanId);

    @Query("""
            select count(leg) > 0
            from SplitDeliveryLeg leg
            where leg.driver.id = :driverId
              and leg.status in :statuses
              and (:excludedPlanId is null or leg.splitPlan.id <> :excludedPlanId)
            """)
    boolean existsActiveDriverLeg(@Param("driverId") Long driverId,
                                  @Param("statuses") Collection<SplitDeliveryPlanStatus> statuses,
                                  @Param("excludedPlanId") Long excludedPlanId);
}
