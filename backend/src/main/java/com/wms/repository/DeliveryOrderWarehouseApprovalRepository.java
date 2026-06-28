package com.wms.repository;

import com.wms.entity.DeliveryOrderWarehouseApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderWarehouseApprovalRepository extends JpaRepository<DeliveryOrderWarehouseApproval, Long> {
}
