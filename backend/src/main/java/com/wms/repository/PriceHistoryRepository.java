package com.wms.repository;

import com.wms.entity.PriceHistory;
import com.wms.enums.PriceHistoryStatus;
<<<<<<< HEAD
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
=======
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

    /** Approved entries for overlap check — scoped to (product, warehouse), excludes the entry being edited. */
    @Query("""
        SELECT p FROM PriceHistory p
        WHERE p.product.id = :productId
          AND p.warehouse.id = :warehouseId
          AND p.status = 'APPROVED'
          AND p.effectiveDate <= :endDate
          AND p.endDate >= :effectiveDate
          AND (:excludeId IS NULL OR p.id <> :excludeId)
        """)
    List<PriceHistory> findApprovedOverlapping(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("effectiveDate") LocalDate effectiveDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    /** Price lookup at a given date for DO creation — scoped to the DO's warehouse. */
    @Query("""
        SELECT p FROM PriceHistory p
        WHERE p.product.id = :productId
          AND p.warehouse.id = :warehouseId
          AND p.status = 'APPROVED'
          AND p.effectiveDate <= :date
          AND p.endDate >= :date
        """)
    Optional<PriceHistory> findApprovedAtDate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("date") LocalDate date);

    /** Most recent approved entry for a (product, warehouse) pair — used in approval delta view. */
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
>>>>>>> main
}
