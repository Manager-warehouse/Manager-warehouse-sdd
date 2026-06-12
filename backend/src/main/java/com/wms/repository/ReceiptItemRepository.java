package com.wms.repository;

import com.wms.entity.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {

    List<ReceiptItem> findByReceiptId(Long receiptId);

    Optional<ReceiptItem> findByIdAndReceiptId(Long id, Long receiptId);

    @Query("SELECT COUNT(r) FROM Receipt r WHERE r.supplier.id = :supplierId AND r.status = 'APPROVED'")
    long countApprovedReceiptsBySupplierId(@Param("supplierId") Long supplierId);
}
