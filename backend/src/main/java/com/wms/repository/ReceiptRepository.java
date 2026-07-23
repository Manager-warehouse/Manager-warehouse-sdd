package com.wms.repository;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findBySupplierIdOrderByDocumentDateDescCreatedAtDesc(Long supplierId);

    /**
     * Locks the receipt row for the duration of the enclosing write
     * transaction so two concurrent approve/reject/QC/RTV actions on the
     * same receipt cannot both read the pre-mutation state and race past
     * each other before either commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Receipt r where r.id = :id")
    Optional<Receipt> findByIdForUpdate(@Param("id") Long id);

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
