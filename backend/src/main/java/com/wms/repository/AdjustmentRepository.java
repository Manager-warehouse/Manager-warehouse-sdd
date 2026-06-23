package com.wms.repository;

import com.wms.entity.Adjustment;
import com.wms.enums.AdjustmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

        /**
         * Check whether a pending or confirmed RTV adjustment already exists for a
         * given receipt.
         * Used to enforce one-RTV-per-receipt rule (HTTP 409 if duplicate).
         */
        boolean existsByReferenceTypeAndReferenceIdAndType(
                        String referenceType,
                        Long referenceId,
                        AdjustmentType type);

        /**
         * Find the pending RTV adjustment for a receipt to confirm.
         * Pending = approvedAt IS NULL.
         */
        @Query("SELECT a FROM Adjustment a " +
                        "WHERE a.referenceType = :referenceType " +
                        "AND a.referenceId = :referenceId " +
                        "AND a.type = :type " +
                        "AND a.approvedAt IS NULL")
        Optional<Adjustment> findPendingRtvByReference(
                        @Param("referenceType") String referenceType,
                        @Param("referenceId") Long referenceId,
                        @Param("type") AdjustmentType type);

        /**
         * Find the confirmed RTV adjustment for a receipt (approvedAt IS NOT NULL).
         * Used to reject duplicate confirmation attempts (HTTP 409).
         */
        @Query("SELECT a FROM Adjustment a " +
                        "WHERE a.referenceType = :referenceType " +
                        "AND a.referenceId = :referenceId " +
                        "AND a.type = :type " +
                        "AND a.approvedAt IS NOT NULL")
        Optional<Adjustment> findConfirmedRtvByReference(
                        @Param("referenceType") String referenceType,
                        @Param("referenceId") Long referenceId,
                        @Param("type") AdjustmentType type);
}
