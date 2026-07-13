package com.wms.repository;

import com.wms.entity.WrongSkuReportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WrongSkuReportItemRepository extends JpaRepository<WrongSkuReportItem, Long> {
}
