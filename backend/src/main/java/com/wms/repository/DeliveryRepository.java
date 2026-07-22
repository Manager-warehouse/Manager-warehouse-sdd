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
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.enums.order_fulfillment.DeliveryStatus;
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

    Optional<Delivery> findFirstByDeliveryOrderIdOrderByCreatedAtDesc(Long doId);
}
