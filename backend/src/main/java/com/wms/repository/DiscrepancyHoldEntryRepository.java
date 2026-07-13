package com.wms.repository;

import com.wms.entity.DiscrepancyHoldEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscrepancyHoldEntryRepository extends JpaRepository<DiscrepancyHoldEntry, Long> {
}
