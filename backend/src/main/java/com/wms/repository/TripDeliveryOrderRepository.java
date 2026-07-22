package com.wms.repository;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.order_fulfillment.TripDeliveryOrder;
import com.wms.enums.order_fulfillment.TripStatus;
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

    @Modifying
    @Query("""
            update TripDeliveryOrder tdo
            set tdo.isActive = false
            where tdo.trip.id = :tripId
            """)
    void deactivateByTripId(@Param("tripId") Long tripId);

    @EntityGraph(attributePaths = {"trip", "deliveryOrder"})
    @Query("""
            select tdo from TripDeliveryOrder tdo
            where tdo.deliveryOrder.id in :deliveryOrderIds
              and tdo.isActive = true
              and tdo.trip.status in :activeStatuses
              and (:excludedTripId is null or tdo.trip.id <> :excludedTripId)
            """)
    List<TripDeliveryOrder> findAssignmentsForDeliveryOrders(
            @Param("deliveryOrderIds") Collection<Long> deliveryOrderIds,
            @Param("activeStatuses") Collection<TripStatus> activeStatuses,
            @Param("excludedTripId") Long excludedTripId);

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
