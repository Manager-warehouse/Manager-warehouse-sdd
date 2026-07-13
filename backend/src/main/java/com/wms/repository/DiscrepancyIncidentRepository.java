package com.wms.repository;

import com.wms.entity.DiscrepancyIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscrepancyIncidentRepository extends JpaRepository<DiscrepancyIncident, Long> {
}
