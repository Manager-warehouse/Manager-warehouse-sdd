package com.wms.repository;

import com.wms.entity.InterWarehouseTransfer;
import com.wms.enums.InterWarehouseTransferStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
