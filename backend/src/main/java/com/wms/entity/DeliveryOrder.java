package com.wms.entity;

import lombok.*;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "delivery_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "do_number", nullable = false, unique = true, length = 50)
    private String doNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dealer_id", nullable = false)
    private Dealer dealer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DeliveryOrderType type;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packed_by")
    private User packedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_by")
    private User qcBy;
}
