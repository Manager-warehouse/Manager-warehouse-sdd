package com.wms.repository;

import com.wms.entity.DeliveryOrderItemReplacement;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderItemReplacementRepository extends JpaRepository<DeliveryOrderItemReplacement, Long> {

    @Query("""
            select coalesce(sum(r.quantity), 0)
            from DeliveryOrderItemReplacement r
            where r.deliveryOrderItem.id = :deliveryOrderItemId
            """)
    BigDecimal sumReplacementQtyByDeliveryOrderItemId(@Param("deliveryOrderItemId") Long deliveryOrderItemId);
}
