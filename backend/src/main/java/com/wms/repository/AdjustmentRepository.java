package com.wms.repository;

import com.wms.entity.Adjustment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

    @Query("""
            select a from Adjustment a
            where a.deliveryOrder.id = :deliveryOrderId
              and a.type = com.wms.enums.AdjustmentType.QC_FAIL_OUTBOUND
            order by a.id asc
            """)
    List<Adjustment> findOutboundQcFailAdjustments(@Param("deliveryOrderId") Long deliveryOrderId);
}
