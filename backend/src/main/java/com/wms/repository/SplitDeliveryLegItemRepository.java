package com.wms.repository;

import com.wms.entity.order_fulfillment.SplitDeliveryLegItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SplitDeliveryLegItemRepository extends JpaRepository<SplitDeliveryLegItem, Long> {

    @EntityGraph(attributePaths = {"splitLeg", "deliveryOrderItem", "product", "batch"})
    List<SplitDeliveryLegItem> findBySplitLegIdIn(Collection<Long> splitLegIds);
}
