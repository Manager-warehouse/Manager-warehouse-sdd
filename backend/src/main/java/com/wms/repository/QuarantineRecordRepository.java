package com.wms.repository;

import com.wms.entity.QuarantineRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuarantineRecordRepository extends JpaRepository<QuarantineRecord, Long> {

    @Query("""
            select q from QuarantineRecord q
            where q.deliveryOrder.id = :deliveryOrderId
            order by q.id asc
            """)
    List<QuarantineRecord> findByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);
}
