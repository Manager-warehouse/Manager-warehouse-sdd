package com.wms.repository;

import com.wms.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    @Query("select coalesce(max(d.attemptNumber), 0) from Delivery d where d.deliveryOrder.id = :deliveryOrderId")
    Integer findMaxAttemptNumberByDeliveryOrderId(@Param("deliveryOrderId") Long deliveryOrderId);
}
