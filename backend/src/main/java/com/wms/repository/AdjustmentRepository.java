package com.wms.repository;


import com.wms.entity.stock_control.Adjustment;
import com.wms.enums.stock_control.AdjustmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    java.util.List<Adjustment> findByTypeAndApprovedAtIsNull(AdjustmentType type);

    List<Adjustment> findByReferenceTypeAndReferenceIdAndTypeOrderByIdAsc(
            String referenceType,
            Long referenceId,
            AdjustmentType type);

    Optional<Adjustment> findByAdjustmentNumber(String adjustmentNumber);

    Optional<Adjustment> findByOutboundQcRecordId(Long outboundQcRecordId);

    /**
     * List correction vouchers (type = CORRECTION_VOUCHER), optionally filtered by
     * referenceType, newest first. Used by GET /api/v1/correction-vouchers.
     */
    @Query("SELECT a FROM Adjustment a WHERE a.type = :type " +
            "AND (:referenceType IS NULL OR a.referenceType = :referenceType) " +
            "ORDER BY a.createdAt DESC")
    java.util.List<Adjustment> findByTypeAndOptionalReferenceType(
            @Param("type") AdjustmentType type,
            @Param("referenceType") String referenceType);
}
