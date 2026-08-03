package com.wms.repository;


import com.wms.entity.warehouse_transfer.DiscrepancyIncident;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscrepancyIncidentRepository extends JpaRepository<DiscrepancyIncident, Long> {
    @EntityGraph(attributePaths = {
            "transfer",
            "transfer.sourceWarehouse",
            "transfer.destinationWarehouse",
            "product",
            "resolvedBy"
    })
    @Query("select incident from DiscrepancyIncident incident")
    List<DiscrepancyIncident> findAllWithDetails(Sort sort);

    @EntityGraph(attributePaths = {
            "transfer",
            "transfer.sourceWarehouse",
            "transfer.destinationWarehouse",
            "product",
            "resolvedBy"
    })
    List<DiscrepancyIncident> findByStatus(String status, Sort sort);

    @EntityGraph(attributePaths = {
            "transfer",
            "transfer.sourceWarehouse",
            "transfer.destinationWarehouse",
            "product",
            "resolvedBy"
    })
    @Query("select incident from DiscrepancyIncident incident where incident.id = :id")
    Optional<DiscrepancyIncident> findWithDetailsById(@Param("id") Long id);
}
