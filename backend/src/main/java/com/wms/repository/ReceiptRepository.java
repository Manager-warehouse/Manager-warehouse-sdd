package com.wms.repository;

import com.wms.entity.Receipt;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findBySupplierIdOrderByDocumentDateDescCreatedAtDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    Optional<Receipt> findByIdAndSupplierId(Long id, Long supplierId);

    @EntityGraph(attributePaths = {"warehouse"})
    @Query("select r from Receipt r where r.id = :id")
    Optional<Receipt> findByIdWithWarehouse(@Param("id") Long id);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    @Query("select r from Receipt r where r.warehouse.id = :warehouseId order by r.documentDate desc, r.createdAt desc")
    List<Receipt> findByWarehouseIdOrderByDocumentDateDescCreatedAtDesc(@Param("warehouseId") Long warehouseId);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    @Query("select r from Receipt r where r.warehouse.id = :warehouseId and (:type is null or r.type = :type) order by r.documentDate desc, r.createdAt desc")
    List<Receipt> findByWarehouseIdAndTypeOrderByDocumentDateDescCreatedAtDesc(@Param("warehouseId") Long warehouseId, @Param("type") ReceiptType type);

    @EntityGraph(attributePaths = {"supplier", "warehouse"})
    @Query("select r from Receipt r where r.id = :id")
    Optional<Receipt> findByIdWithSupplierAndWarehouse(@Param("id") Long id);

    boolean existsBySupplierIdAndWarehouseIdAndSourceOrderCodeAndTypeAndStatusNot(
            Long supplierId,
            Long warehouseId,
            String sourceOrderCode,
            ReceiptType type,
            ReceiptStatus status);

    boolean existsByReceiptNumber(String receiptNumber);

    List<Receipt> findByDeliveryOrderIdAndType(Long deliveryOrderId, ReceiptType type);

    long countByWarehouseIdAndDocumentDate(Long warehouseId, LocalDate documentDate);
}
