package com.wms.repository;

import com.wms.entity.Delivery;
import com.wms.enums.DeliveryStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Query("select coalesce(max(d.attemptNumber), 0) from Delivery d where d.deliveryOrder.id = :deliveryOrderId")
    Integer findMaxAttemptNumberByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrder.dealer", "deliveryOrder.warehouse",
            "trip", "driver", "driver.user", "vehicle"
    })
    @Query("""
            select d from Delivery d
            where d.trip.id = :tripId
              and d.deliveryOrder.id = :deliveryOrderId
              and d.driver.id = :driverId
              and d.status in :statuses
            order by d.attemptNumber desc
            limit 1
            """)
    Optional<Delivery> findCurrentAttempt(@Param("tripId") Long tripId,
                                          @Param("deliveryOrderId") Long deliveryOrderId,
                                          @Param("driverId") Long driverId,
                                          @Param("statuses") Collection<DeliveryStatus> statuses);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrder.dealer", "deliveryOrder.warehouse",
            "trip", "driver", "driver.user", "vehicle"
    })
    @Query("""
            select d from Delivery d
            where d.deliveryOrder.id = :deliveryOrderId
              and d.status in :statuses
            order by d.attemptNumber desc
            limit 1
            """)
    Optional<Delivery> findLatestCurrentAttemptByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId,
                                                                 @Param("statuses") Collection<DeliveryStatus> statuses);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrder.dealer", "deliveryOrder.warehouse",
            "trip", "driver", "driver.user", "vehicle"
    })
    @Query("""
            select d from Delivery d
            where d.trip.id = :tripId
              and d.deliveryOrder.id in :deliveryOrderIds
            order by d.deliveryOrder.id asc, d.attemptNumber desc
            """)
    java.util.List<Delivery> findByTripIdAndDeliveryOrderIdIn(@Param("tripId") Long tripId,
                                                              @Param("deliveryOrderIds") Collection<Long> deliveryOrderIds);
}
