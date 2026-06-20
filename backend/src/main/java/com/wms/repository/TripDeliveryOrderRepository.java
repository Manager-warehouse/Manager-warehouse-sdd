package com.wms.repository;

import com.wms.entity.TripDeliveryOrder;
import com.wms.enums.TripStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripDeliveryOrderRepository extends JpaRepository<TripDeliveryOrder, Long> {

    @EntityGraph(attributePaths = {"trip", "deliveryOrder", "deliveryOrder.warehouse"})
    List<TripDeliveryOrder> findByTripIdOrderByStopOrderAsc(Long tripId);

    @EntityGraph(attributePaths = {"trip", "deliveryOrder", "deliveryOrder.dealer", "deliveryOrder.warehouse"})
    @Query("""
            select tdo from TripDeliveryOrder tdo
            where tdo.trip.id = :tripId
              and tdo.deliveryOrder.id = :deliveryOrderId
            """)
    java.util.Optional<TripDeliveryOrder> findByTripIdAndDeliveryOrderId(@Param("tripId") Long tripId,
                                                                         @Param("deliveryOrderId") Long deliveryOrderId);

    @Modifying
    void deleteByTripId(Long tripId);

    @Query("""
            select count(tdo) > 0 from TripDeliveryOrder tdo
            where tdo.deliveryOrder.id in :deliveryOrderIds
              and tdo.trip.status in :statuses
              and (:excludedTripId is null or tdo.trip.id <> :excludedTripId)
            """)
    boolean existsActiveAssignmentForAnyDeliveryOrder(@Param("deliveryOrderIds") Collection<Long> deliveryOrderIds,
                                                      @Param("statuses") Collection<TripStatus> statuses,
                                                      @Param("excludedTripId") Long excludedTripId);
}
