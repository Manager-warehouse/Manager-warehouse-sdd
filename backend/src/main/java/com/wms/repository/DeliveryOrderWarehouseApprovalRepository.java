package com.wms.repository;


import com.wms.entity.order_fulfillment.DeliveryOrderWarehouseApproval;
import com.wms.enums.order_fulfillment.ApprovalResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderWarehouseApprovalRepository extends JpaRepository<DeliveryOrderWarehouseApproval, Long> {

    @EntityGraph(attributePaths = {"approver"})
    Optional<DeliveryOrderWarehouseApproval> findFirstByDeliveryOrderIdAndResultOrderByApprovedAtDesc(
            Long deliveryOrderId,
            ApprovalResult result);
}
