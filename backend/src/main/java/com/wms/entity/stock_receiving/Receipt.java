package com.wms.entity.stock_receiving;


import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 50)
    private String receiptNumber;

    @Column(name = "source_order_code", length = 100)
    private String sourceOrderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ReceiptType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id")
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id")
    private DeliveryOrder deliveryOrder;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "source_channel", length = 50)
    private String sourceChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ReceiptStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_receive_approved_by")
    private User preReceiveApprovedBy;

    @Column(name = "pre_receive_approved_at")
    private OffsetDateTime preReceiveApprovedAt;

    @Column(name = "pre_receive_rejection_reason", columnDefinition = "TEXT")
    private String preReceiveRejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storekeeper_reviewed_by")
    private User storekeeperReviewedBy;

    @Column(name = "storekeeper_reviewed_at")
    private OffsetDateTime storekeeperReviewedAt;

    @Column(name = "recount_reason", columnDefinition = "TEXT")
    private String recountReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "putaway_completed_at")
    private OffsetDateTime putawayCompletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
