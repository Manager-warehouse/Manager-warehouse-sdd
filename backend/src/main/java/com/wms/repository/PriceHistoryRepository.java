package com.wms.repository;

import com.wms.entity.PriceHistory;
import com.wms.enums.PriceHistoryStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    @Query("""
            select p from PriceHistory p
            where p.product.id = :productId
              and p.status = :status
              and p.effectiveDate <= :asOfDate
              and (p.endDate is null or p.endDate >= :asOfDate)
            order by p.effectiveDate desc
            """)
    List<PriceHistory> findEffectivePrices(@Param("productId") Long productId,
                                           @Param("status") PriceHistoryStatus status,
                                           @Param("asOfDate") LocalDate asOfDate);
}
