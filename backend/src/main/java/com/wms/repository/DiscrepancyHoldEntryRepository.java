package com.wms.repository;


import com.wms.entity.warehouse_transfer.DiscrepancyHoldEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscrepancyHoldEntryRepository extends JpaRepository<DiscrepancyHoldEntry, Long> {
    List<DiscrepancyHoldEntry> findByIncidentId(Long incidentId);
}
