package com.wms.entity.stock_control;


import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation;
import com.wms.entity.order_fulfillment.OutboundQcRecord;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.QuarantineRecord;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.stock_control.AdjustmentStatus;
import com.wms.enums.stock_control.AdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adjustment_number", nullable = false, unique = true, length = 50)
    private String adjustmentNumber;

    // Nullable at the JPA level too: required for the 4 inventory adjustment types
    // (enforced by the DB's chk_adjustments_inventory_fields_required CHECK), but
    // absent for type = CORRECTION_VOUCHER, which has no warehouse/product context.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id")
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_item_id")
    private DeliveryOrderItem deliveryOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id")
    private DeliveryOrderItemAllocation allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_qc_record_id")
    private OutboundQcRecord outboundQcRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_record_id")
    private QuarantineRecord quarantineRecord;

    // Nullable for type = CORRECTION_VOUCHER (see chk_adjustments_inventory_fields_required).
    @Column(name = "quantity_adjustment", precision = 10, scale = 2)
    private BigDecimal quantityAdjustment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private AdjustmentType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AdjustmentStatus status = AdjustmentStatus.PENDING_APPROVAL;

    // Signed monetary delta for type = CORRECTION_VOUCHER only; applied to
    // dealers.current_balance / suppliers.current_balance. Not reused from
    // quantityAdjustment, which is a unit count rather than a currency amount.
    @Column(name = "amount_delta", precision = 18, scale = 2)
    private BigDecimal amountDelta;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void syncStatusFromApprovalFields() {
        if (approvedAt != null && status == AdjustmentStatus.PENDING_APPROVAL) {
            status = AdjustmentStatus.APPROVED;
        }
    }
}
