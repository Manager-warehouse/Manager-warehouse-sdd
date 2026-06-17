package com.wms.repository;

import com.wms.entity.TransferItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferItemRepository extends JpaRepository<TransferItem, Long> {
    List<TransferItem> findByTransferIdOrderById(Long transferId);
    void deleteByTransferId(Long transferId);
}
