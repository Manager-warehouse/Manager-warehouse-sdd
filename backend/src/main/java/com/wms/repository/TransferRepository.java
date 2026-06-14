package com.wms.repository;

import com.wms.entity.Transfer;
import com.wms.enums.TransferStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    boolean existsByTransferNumber(String transferNumber);

    List<Transfer> findBySourceWarehouseIdAndStatus(Long sourceWarehouseId, TransferStatus status);

    @EntityGraph(attributePaths = {
            "sourceWarehouse",
            "destinationWarehouse",
            "createdBy",
            "approvedBy",
            "rejectedBy",
            "confirmedBy",
            "trip",
            "accountingPeriod"
    })
    @Query("select t from Transfer t where t.id = :id")
    Optional<Transfer> findDetailedById(@Param("id") Long id);
}
