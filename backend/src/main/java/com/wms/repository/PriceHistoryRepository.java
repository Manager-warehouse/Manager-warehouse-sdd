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

        List<PriceHistory> findByProductIdAndWarehouseIdOrderByCreatedAtDesc(Long productId, Long warehouseId);

        List<PriceHistory> findByProductIdOrderByCreatedAtDesc(Long productId);

        List<PriceHistory> findByStatusOrderByCreatedAtAsc(PriceHistoryStatus status);

        List<PriceHistory> findByWarehouseIdAndStatusOrderByCreatedAtAsc(Long warehouseId, PriceHistoryStatus status);

        List<PriceHistory> findByProductIdAndWarehouseIdAndStatusOrderByCreatedAtAsc(
                        Long productId, Long warehouseId, PriceHistoryStatus status);

        List<PriceHistory> findByProductIdAndStatusOrderByCreatedAtAsc(Long productId, PriceHistoryStatus status);

        List<PriceHistory> findByWarehouseIdOrderByCreatedAtAsc(Long warehouseId);

        List<PriceHistory> findAllByOrderByCreatedAtAsc();

        /**
         * APPROVED entries conflicting with a candidate effective_date for
         * (product, warehouse) — effective-date-only model, no more range overlap.
         * Excludes the entry being edited when excludeId is provided.
         */
        @Query("""
                        SELECT p FROM PriceHistory p
                        WHERE p.product.id = :productId
                          AND p.warehouse.id = :warehouseId
                          AND p.status = 'APPROVED'
                          AND p.effectiveDate = :effectiveDate
                          AND (:excludeId IS NULL OR p.id <> :excludeId)
                        """)
        List<PriceHistory> findConflictingApproved(
                        @Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId,
                        @Param("effectiveDate") LocalDate effectiveDate,
                        @Param("excludeId") Long excludeId);

        /**
         * Price lookup at a given date for DO creation — the APPROVED entry with the
         * largest effective_date not after the given date (latest entry supersedes
         * all earlier ones until a newer APPROVED entry exists).
         */
        Optional<PriceHistory> findFirstByProductIdAndWarehouseIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        Long productId, Long warehouseId, PriceHistoryStatus status, LocalDate date);

        /**
         * Most recent approved entry for a (product, warehouse) pair — used in approval
         * delta view.
         */
        @Query("""
                        SELECT p FROM PriceHistory p
                        WHERE p.product.id = :productId
                          AND p.warehouse.id = :warehouseId
                          AND p.status = 'APPROVED'
                        ORDER BY p.effectiveDate DESC
                        """)
        List<PriceHistory> findLatestApproved(
                        @Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId);
}
