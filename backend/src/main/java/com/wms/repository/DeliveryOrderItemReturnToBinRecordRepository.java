package com.wms.repository;


import com.wms.entity.order_fulfillment.DeliveryOrderItemReturnToBinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemReturnToBinRecordRepository
        extends JpaRepository<DeliveryOrderItemReturnToBinRecord, Long> {
}
