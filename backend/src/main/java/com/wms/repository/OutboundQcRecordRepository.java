package com.wms.repository;


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

    List<OutboundQcRecord> findAllByIsActiveTrueAndInventoryMovedAtIsNotNull();

    boolean existsByAllocationIdAndIsActiveTrue(Long allocationId);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    @Query("select r from OutboundQcRecord r where r.allocation.id in :allocationIds and r.isActive = true")
    List<OutboundQcRecord> findByAllocationIdIn(@Param("allocationIds") Collection<Long> allocationIds);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    List<OutboundQcRecord> findByDeliveryOrderIdAndIsActiveTrue(Long deliveryOrderId);

    @EntityGraph(attributePaths = {
            "deliveryOrder", "deliveryOrderItem", "allocation", "batch", "location", "zone",
            "stagingLocation", "quarantineLocation", "quarantineRecord"
    })
    @Query("""
            select r from OutboundQcRecord r
            where r.deliveryOrder.id = :deliveryOrderId
              and r.idempotencyKey = :idempotencyKey
              and r.isActive = true
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
              and r.isActive = true
              and r.inventoryMovedAt is not null
            order by r.deliveryOrder.id asc, r.id asc
            """)
    List<OutboundQcRecord> findPassedRecordsByDeliveryOrderIdIn(@Param("deliveryOrderIds") Collection<Long> deliveryOrderIds);

    @Query("""
            select r from OutboundQcRecord r
            where r.deliveryOrder.warehouse.id = :warehouseId
              and r.isActive = true
              and r.inventoryMovedAt is not null
              and r.createdAt >= :start
              and r.createdAt <= :end
            """)
    List<OutboundQcRecord> findByWarehouseIdAndCreatedAtBetween(@Param("warehouseId") Long warehouseId, @Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);
}

