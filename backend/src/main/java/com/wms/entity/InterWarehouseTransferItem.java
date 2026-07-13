package com.wms.entity;

import com.wms.enums.QcResult;
import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "inter_warehouse_transfer_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterWarehouseTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private InterWarehouseTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private WarehouseLocation sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private WarehouseLocation destinationLocation;

    @Column(name = "planned_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "sent_qty", precision = 10, scale = 2)
    private BigDecimal sentQty;

    @Column(name = "received_qty", precision = 10, scale = 2)
    private BigDecimal receivedQty;

    @Column(name = "worker_received_qty", precision = 10, scale = 2)
    private BigDecimal workerReceivedQty;

    @Column(name = "issue_reason", columnDefinition = "TEXT")
    private String issueReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private User checkedBy;

    @Column(name = "checked_at")
    private java.time.OffsetDateTime checkedAt;

    @Column(name = "checker_note", columnDefinition = "TEXT")
    private String checkerNote;

    @Column(name = "qc_passed_qty", precision = 10, scale = 2)
    private BigDecimal qcPassedQty;

    @Column(name = "qc_failed_qty", precision = 10, scale = 2)
    private BigDecimal qcFailedQty;

    @Column(name = "qc_failure_reason", columnDefinition = "TEXT")
    private String qcFailureReason;

    @Column(name = "variance_qty", precision = 10, scale = 2)
    private BigDecimal varianceQty;
}
