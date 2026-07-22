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
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
    boolean existsByDoNumber(String doNumber);
    List<DeliveryOrder> findByDealerIdAndStatus(Long dealerId, DeliveryOrderStatus status);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("select d from DeliveryOrder d where d.id = :id")
    Optional<DeliveryOrder> findWithDealerAndWarehouseById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("""
            select d from DeliveryOrder d
            order by d.updatedAt desc
            """)
    List<DeliveryOrder> findAllDetailedOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.warehouse.id in :warehouseIds
            order by d.updatedAt desc
            """)
    List<DeliveryOrder> findDetailedByWarehouseIdIn(@Param("warehouseIds") Collection<Long> warehouseIds);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.id = :id
              and d.status in :statuses
            """)
    Optional<DeliveryOrder> findWithDealerAndWarehouseByIdAndStatusIn(@Param("id") Long id,
                                                                       @Param("statuses") Collection<DeliveryOrderStatus> statuses);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy", "packedBy", "qcBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.warehouse.id = :warehouseId
              and d.status in :statuses
            order by d.updatedAt desc
            """)
    List<DeliveryOrder> findDetailedByWarehouseIdAndStatusIn(@Param("warehouseId") Long warehouseId,
                                                             @Param("statuses") Collection<DeliveryOrderStatus> statuses);

    @EntityGraph(attributePaths = {"dealer", "warehouse", "createdBy"})
    @Query("""
            select d from DeliveryOrder d
            where d.id in :ids
            """)
    List<DeliveryOrder> findDetailedByIdIn(@Param("ids") Collection<Long> ids);

    long countByWarehouseIdAndDocumentDate(Long warehouseId, LocalDate documentDate);
}
