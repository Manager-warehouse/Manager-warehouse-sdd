package com.wms.repository;

import com.wms.entity.TransferItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferItemRepository extends JpaRepository<TransferItem, Long> {
    @EntityGraph(attributePaths = {"product", "batch", "sourceLocation", "destinationLocation"})
    List<TransferItem> findByTransferIdOrderById(Long transferId);

    @EntityGraph(attributePaths = {"product", "batch", "sourceLocation", "destinationLocation"})
    List<TransferItem> findByTransferId(Long transferId);

    void deleteByTransferId(Long transferId);
}
