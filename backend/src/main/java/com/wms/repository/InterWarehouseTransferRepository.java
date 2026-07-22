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
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
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
public interface InterWarehouseTransferRepository extends JpaRepository<InterWarehouseTransfer, Long> {
    boolean existsByTransferNumber(String transferNumber);

    long countByTripIdAndIdNot(Long tripId, Long transferId);

    long countByTripId(Long tripId);

    boolean existsByTransferRequestIdAndStatusNotIn(Long transferRequestId, Collection<InterWarehouseTransferStatus> statuses);

    boolean existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn(
            String externalInstructionCode,
            Long sourceWarehouseId,
            Long destinationWarehouseId,
            LocalDate documentDate,
            Collection<InterWarehouseTransferStatus> statuses);

    boolean existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot(
            String externalInstructionCode,
            Long sourceWarehouseId,
            Long destinationWarehouseId,
            LocalDate documentDate,
            Collection<InterWarehouseTransferStatus> statuses,
            Long id);

    @Query("""
        SELECT DISTINCT t FROM InterWarehouseTransfer t
        LEFT JOIN FETCH t.items items
        LEFT JOIN FETCH items.product
        LEFT JOIN FETCH items.batch
        LEFT JOIN FETCH items.sourceLocation
        LEFT JOIN FETCH items.destinationLocation
        LEFT JOIN FETCH t.sourceWarehouse
        LEFT JOIN FETCH t.destinationWarehouse
        LEFT JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.approvedBy
        LEFT JOIN FETCH t.rejectedBy
        LEFT JOIN FETCH t.confirmedBy
        LEFT JOIN FETCH t.trip
        LEFT JOIN FETCH t.accountingPeriod
        ORDER BY t.createdAt DESC
        """)
    List<InterWarehouseTransfer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"sourceWarehouse", "destinationWarehouse", "createdBy", "approvedBy", "rejectedBy", "confirmedBy", "trip", "accountingPeriod"})
    @Query("select t from InterWarehouseTransfer t where t.id = :id")
    Optional<InterWarehouseTransfer> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"sourceWarehouse", "destinationWarehouse", "trip", "items"})
    @Query("select distinct t from InterWarehouseTransfer t where t.trip.id in :tripIds")
    List<InterWarehouseTransfer> findByTripIdInWithSummary(@Param("tripIds") Collection<Long> tripIds);

    @EntityGraph(attributePaths = {"sourceWarehouse", "destinationWarehouse", "trip", "items"})
    @Query("select t from InterWarehouseTransfer t where t.trip.id = :tripId")
    Optional<InterWarehouseTransfer> findByTripIdWithSummary(@Param("tripId") Long tripId);
}
