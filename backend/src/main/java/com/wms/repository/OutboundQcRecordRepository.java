package com.wms.repository;

import com.wms.entity.OutboundQcRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboundQcRecordRepository extends JpaRepository<OutboundQcRecord, Long> {

    boolean existsByAllocationId(Long allocationId);
}
