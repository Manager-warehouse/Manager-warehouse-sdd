package com.wms.repository;

import com.wms.entity.TransferAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferAllocationRepository extends JpaRepository<TransferAllocation, Long> {
    List<TransferAllocation> findByTransferItemId(Long transferItemId);
    List<TransferAllocation> findByTransferItemTransferId(Long transferId);
    void deleteByTransferItemTransferId(Long transferId);
}
