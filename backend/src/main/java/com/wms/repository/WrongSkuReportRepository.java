package com.wms.repository;

import com.wms.entity.WrongSkuReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WrongSkuReportRepository extends JpaRepository<WrongSkuReport, Long> {
    List<WrongSkuReport> findByTransferId(Long transferId);
}
