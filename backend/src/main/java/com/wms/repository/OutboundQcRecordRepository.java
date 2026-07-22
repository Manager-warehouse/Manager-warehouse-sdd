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
import com.wms.entity.order_fulfillment.OutboundQcRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundQcRecordRepository extends JpaRepository<OutboundQcRecord, Long> {

    boolean existsByAllocationId(Long allocationId);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    List<OutboundQcRecord> findByAllocationIdIn(Collection<Long> allocationIds);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    @Query("""
            select r from OutboundQcRecord r
            where r.deliveryOrder.id = :deliveryOrderId
              and r.idempotencyKey = :idempotencyKey
            order by r.id asc
            """)
    List<OutboundQcRecord> findByDeliveryOrderIdAndIdempotencyKey(@Param("deliveryOrderId") Long deliveryOrderId,
                                                                  @Param("idempotencyKey") String idempotencyKey);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    @Query("""
            select r from OutboundQcRecord r
            where r.deliveryOrder.id in :deliveryOrderIds
              and r.qcPassQty > 0
            order by r.deliveryOrder.id asc, r.id asc
            """)
    List<OutboundQcRecord> findPassedRecordsByDeliveryOrderIdIn(@Param("deliveryOrderIds") Collection<Long> deliveryOrderIds);

    @Query("""
            select r from OutboundQcRecord r
            where r.deliveryOrder.warehouse.id = :warehouseId
              and r.createdAt >= :start
              and r.createdAt <= :end
            """)
    List<OutboundQcRecord> findByWarehouseIdAndCreatedAtBetween(@Param("warehouseId") Long warehouseId, @Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);
}

