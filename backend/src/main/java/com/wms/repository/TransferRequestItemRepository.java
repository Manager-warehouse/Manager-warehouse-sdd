package com.wms.repository;

import com.wms.entity.TransferRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestItemRepository extends JpaRepository<TransferRequestItem, Long> {
    List<TransferRequestItem> findByTransferRequestId(Long transferRequestId);
    void deleteByTransferRequestId(Long transferRequestId);
}
