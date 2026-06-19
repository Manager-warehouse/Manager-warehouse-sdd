package com.wms.repository;

import com.wms.entity.DeliveryOrderItemReturnToBinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemReturnToBinRecordRepository
        extends JpaRepository<DeliveryOrderItemReturnToBinRecord, Long> {
}
