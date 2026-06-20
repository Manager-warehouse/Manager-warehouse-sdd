package com.wms.repository;

import com.wms.entity.OutboundQcRecord;
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
}
