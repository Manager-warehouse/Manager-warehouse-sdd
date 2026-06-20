package com.wms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "outbound_qc_records")
@Getter
@Setter
public class OutboundQcRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_item_id", nullable = false)
    private DeliveryOrderItem deliveryOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private DeliveryOrderItemAllocation allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private WarehouseLocation zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staging_location_id")
    private WarehouseLocation stagingLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_location_id")
    private WarehouseLocation quarantineLocation;

    @Column(name = "picked_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal pickedQty;

    @Column(name = "qc_pass_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal qcPassQty;

    @Column(name = "qc_fail_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal qcFailQty;

    @Column(name = "qc_fail_reason", columnDefinition = "TEXT")
    private String qcFailReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarantine_record_id")
    private QuarantineRecord quarantineRecord;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
