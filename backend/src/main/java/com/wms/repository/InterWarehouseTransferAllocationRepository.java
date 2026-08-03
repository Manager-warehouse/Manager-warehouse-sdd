package com.wms.repository;


import com.wms.entity.warehouse_transfer.InterWarehouseTransferAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterWarehouseTransferAllocationRepository extends JpaRepository<InterWarehouseTransferAllocation, Long> {
    List<InterWarehouseTransferAllocation> findByTransferItemId(Long transferItemId);
    List<InterWarehouseTransferAllocation> findByTransferItemTransferId(Long transferId);
    void deleteByTransferItemId(Long transferItemId);
    void deleteByTransferItemTransferId(Long transferId);
}
