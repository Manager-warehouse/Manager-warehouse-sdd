package com.wms.repository;

import com.wms.entity.Receipt;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
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

    boolean existsBySupplierIdAndWarehouseIdAndSourceOrderCodeAndTypeAndStatusNot(
            Long supplierId,
            Long warehouseId,
            String sourceOrderCode,
            ReceiptType type,
            ReceiptStatus status);

    boolean existsByReceiptNumber(String receiptNumber);
}
