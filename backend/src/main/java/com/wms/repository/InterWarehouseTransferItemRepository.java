package com.wms.repository;

import com.wms.entity.InterWarehouseTransferItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterWarehouseTransferItemRepository extends JpaRepository<InterWarehouseTransferItem, Long> {
    @EntityGraph(attributePaths = {"product", "batch", "sourceLocation", "destinationLocation"})
    List<InterWarehouseTransferItem> findByTransferIdOrderById(Long transferId);

    @EntityGraph(attributePaths = {"product", "batch", "sourceLocation", "destinationLocation"})
    List<InterWarehouseTransferItem> findByTransferId(Long transferId);

    void deleteByTransferId(Long transferId);
}
