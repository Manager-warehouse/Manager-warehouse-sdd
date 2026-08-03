package com.wms.repository;


import com.wms.entity.warehouse_transfer.DamageReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {
    Optional<DamageReport> findByReceiptItemId(Long receiptItemId);
}
