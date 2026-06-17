package com.wms.repository;

import com.wms.entity.PriceHistory;
import com.wms.enums.PriceHistoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long>,
        JpaSpecificationExecutor<PriceHistory> {

    List<PriceHistory> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<PriceHistory> findByStatusOrderByCreatedAtAsc(PriceHistoryStatus status);

    /** Approved entries for overlap check — excludes the entry being edited when id is provided. */
    @Query("""
        SELECT p FROM PriceHistory p
        WHERE p.product.id = :productId
          AND p.status = 'APPROVED'
          AND p.effectiveDate <= :endDate
          AND p.endDate >= :effectiveDate
          AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    List<PriceHistory> findApprovedOverlapping(
            @Param("productId") Long productId,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    /** Price lookup at a given date for DO creation. */
    @Query("""
        SELECT p FROM PriceHistory p
        WHERE p.product.id = :productId
          AND p.status = 'APPROVED'
          AND p.effectiveDate <= :date
          AND p.endDate >= :date
        """)
    Optional<PriceHistory> findApprovedAtDate(
            @Param("productId") Long productId,
            @Param("date") LocalDate date);

    /** Most recent approved entry for a product (used in approval delta view). */
    @Query("""
        SELECT p FROM PriceHistory p
        WHERE p.product.id = :productId
          AND p.status = 'APPROVED'
        ORDER BY p.effectiveDate DESC
        """)
    List<PriceHistory> findLatestApproved(@Param("productId") Long productId);
}
