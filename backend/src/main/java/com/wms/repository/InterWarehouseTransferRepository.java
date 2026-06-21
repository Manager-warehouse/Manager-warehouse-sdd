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

    @EntityGraph(attributePaths = {"sourceWarehouse", "destinationWarehouse", "createdBy", "approvedBy", "rejectedBy", "confirmedBy", "trip", "accountingPeriod"})
    List<InterWarehouseTransfer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"sourceWarehouse", "destinationWarehouse", "createdBy", "approvedBy", "rejectedBy", "confirmedBy", "trip", "accountingPeriod"})
    @Query("select t from InterWarehouseTransfer t where t.id = :id")
    Optional<InterWarehouseTransfer> findWithDetailsById(Long id);
}
