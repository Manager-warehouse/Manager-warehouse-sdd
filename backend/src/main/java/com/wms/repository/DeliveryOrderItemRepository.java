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
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
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
            where i.deliveryOrder.status in (com.wms.enums.order_fulfillment.DeliveryOrderStatus.COMPLETED, com.wms.enums.order_fulfillment.DeliveryOrderStatus.CLOSED)
              and i.deliveryOrder.updatedAt >= :start
              and i.deliveryOrder.updatedAt <= :end
            """)
    List<DeliveryOrderItem> findCompletedItemsInPeriod(@Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);
}

