package com.wms.repository;


import com.wms.entity.warehouse_transfer.WrongSkuReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WrongSkuReportRepository extends JpaRepository<WrongSkuReport, Long> {
    List<WrongSkuReport> findByTransferId(Long transferId);
}
