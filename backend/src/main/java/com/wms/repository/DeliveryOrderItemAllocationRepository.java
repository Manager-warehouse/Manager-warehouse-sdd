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
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
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
    @Query("""
            select a from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.id in :deliveryOrderItemIds
              and a.status = com.wms.enums.stock_control.AllocationStatus.ACTIVE
            """)
    List<DeliveryOrderItemAllocation> findByDeliveryOrderItemIdIn(
            @Param("deliveryOrderItemIds") Collection<Long> deliveryOrderItemIds);

    @EntityGraph(attributePaths = {
            "deliveryOrderItem", "deliveryOrderItem.product", "inventory",
            "batch", "location", "zone", "replacedAllocation"
    })
    @Query("""
            select a from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.deliveryOrder.id = :deliveryOrderId
              and a.status = com.wms.enums.stock_control.AllocationStatus.ACTIVE
              and a.pickedQty < a.plannedQty
            order by a.id asc
            """)
    List<DeliveryOrderItemAllocation> findActiveQcPendingAllocations(@Param("deliveryOrderId") Long deliveryOrderId);

    @Query("""
            select coalesce(sum(a.plannedQty), 0)
            from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.deliveryOrder.id = :deliveryOrderId
              and a.status = com.wms.enums.stock_control.AllocationStatus.ACTIVE
            """)
    BigDecimal sumPlannedQtyByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);

    @Query("""
            select a from DeliveryOrderItemAllocation a
            where a.deliveryOrderItem.deliveryOrder.warehouse.id = :warehouseId
              and a.status = com.wms.enums.stock_control.AllocationStatus.ACTIVE
              and a.createdAt >= :start
              and a.createdAt <= :end
            """)
    List<DeliveryOrderItemAllocation> findByWarehouseIdAndCreatedAtBetween(@Param("warehouseId") Long warehouseId, @Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);
}

