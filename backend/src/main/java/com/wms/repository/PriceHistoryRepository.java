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

        // Used only by getByProduct() (GET /products/{id}/price-history), which is a
        // separate endpoint from the filterable getAll() below (Specification-based).
        List<PriceHistory> findByProductIdOrderByCreatedAtDesc(Long productId);

        /**
         * Non-CANCELLED (PENDING or APPROVED) entries conflicting with a candidate
         * effective_date for (product, warehouse). A PENDING entry already occupying
         * a date blocks creating another one for the same date — the correct way to
         * fix a wrong PENDING entry is to edit it (PUT), not create a duplicate.
         * Excludes the entry being edited when excludeId is provided.
         */
        @Query("""
                        SELECT p FROM PriceHistory p
                        WHERE p.product.id = :productId
                          AND p.warehouse.id = :warehouseId
                          AND p.status IN ('APPROVED', 'PENDING')
                          AND p.effectiveDate = :effectiveDate
                          AND (:excludeId IS NULL OR p.id <> :excludeId)
                        """)
        List<PriceHistory> findConflictingActive(
                        @Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId,
                        @Param("effectiveDate") LocalDate effectiveDate,
                        @Param("excludeId") Long excludeId);

        /**
         * Price lookup at a given date for DO creation — the APPROVED entry with the
         * largest effective_date not after the given date (latest entry supersedes
         * all earlier ones until a newer APPROVED entry exists). Two APPROVED entries
         * sharing an effective_date should no longer be reachable in normal use (the
         * creation-time check above blocks it), but approvedAt DESC still breaks the
         * tie deterministically as defense-in-depth against a create/create race.
         */
        Optional<PriceHistory> findFirstByProductIdAndWarehouseIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescApprovedAtDesc(
                        Long productId, Long warehouseId, PriceHistoryStatus status, LocalDate date);

        /**
         * Most recent approved entry for a (product, warehouse) pair, regardless of any
         * particular date — used where callers want "the current standard cost/price"
         * (e.g. disposal/quarantine valuation), not a comparison anchored to another
         * entry's own effective_date.
         */
        @Query("""
                        SELECT p FROM PriceHistory p
                        WHERE p.product.id = :productId
                          AND p.warehouse.id = :warehouseId
                          AND p.status = 'APPROVED'
                        ORDER BY p.effectiveDate DESC, p.approvedAt DESC
                        """)
        List<PriceHistory> findLatestApproved(
                        @Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId);

        /**
         * The APPROVED entry in effect immediately before a given effective_date, i.e.
         * the entry a PENDING/APPROVED row at that date would supersede — used for the
         * approval delta view so a backdated entry (effective_date earlier than an
         * already-newer APPROVED entry) is compared against the price it actually
         * replaces, not against whatever happens to be the overall most recent one.
         * excludeId keeps an APPROVED entry from matching itself when viewing its own
         * detail.
         */
        @Query("""
                        SELECT p FROM PriceHistory p
                        WHERE p.product.id = :productId
                          AND p.warehouse.id = :warehouseId
                          AND p.status = 'APPROVED'
                          AND p.effectiveDate <= :effectiveDate
                          AND (:excludeId IS NULL OR p.id <> :excludeId)
                        ORDER BY p.effectiveDate DESC, p.approvedAt DESC
                        """)
        List<PriceHistory> findApprovedAtOrBefore(
                        @Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId,
                        @Param("effectiveDate") LocalDate effectiveDate,
                        @Param("excludeId") Long excludeId);
}
