package com.wms.repository;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.entity.stock_control.Adjustment;
import com.wms.enums.stock_control.AdjustmentType;
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

    java.util.List<Adjustment> findByTypeAndApprovedAtIsNull(AdjustmentType type);
}
