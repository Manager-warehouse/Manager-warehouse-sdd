package com.wms.repository;


import com.wms.entity.warehouse_transfer.TransferRequestItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRequestItemRepository extends JpaRepository<TransferRequestItem, Long> {
    List<TransferRequestItem> findByTransferRequestId(Long transferRequestId);
    void deleteByTransferRequestId(Long transferRequestId);
}
