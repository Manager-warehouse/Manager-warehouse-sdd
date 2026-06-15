package com.wms.repository;

import com.wms.entity.ReceiptItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    @EntityGraph(attributePaths = {"product", "batch", "location"})
    List<ReceiptItem> findByReceiptId(Long receiptId);

    List<ReceiptItem> findByReceiptIdAndIdIn(Long receiptId, List<Long> ids);
}
