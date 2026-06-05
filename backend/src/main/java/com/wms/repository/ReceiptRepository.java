package com.wms.repository;

import com.wms.entity.Receipt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findBySupplierIdOrderByDocumentDateDescCreatedAtDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Optional<Receipt> findByIdAndSupplierId(Long id, Long supplierId);
}
