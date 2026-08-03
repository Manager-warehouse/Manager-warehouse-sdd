package com.wms.entity.stock_receiving;


import com.wms.entity.access_control.User;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.stock_receiving.QcResult;
import com.wms.enums.stock_receiving.QcSamplingMethod;
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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @Column(name = "expected_qty", nullable = false)
    private Integer expectedQty;

    @Column(name = "actual_qty")
    private Integer actualQty;

    @Column(name = "over_received_qty", nullable = false)
    @Builder.Default
    private Integer overReceivedQty = 0;

    @Column(name = "sample_qty")
    private Integer sampleQty;

    @Column(name = "sample_passed_qty")
    private Integer samplePassedQty;

    @Column(name = "sample_failed_qty")
    private Integer sampleFailedQty;

    @Column(name = "quality_passed_qty")
    @Builder.Default
    private Integer qualityPassedQty = 0;

    @Column(name = "quality_failed_qty")
    @Builder.Default
    private Integer qualityFailedQty = 0;

    @Column(name = "approved_qty", nullable = false)
    @Builder.Default
    private Integer approvedQty = 0;

    @Column(name = "quarantine_ready_qty", nullable = false)
    @Builder.Default
    private Integer quarantineReadyQty = 0;

    @Column(name = "quarantine_qty", nullable = false)
    @Builder.Default
    private Integer quarantineQty = 0;

    @Column(name = "resolved_quarantine_qty", nullable = false)
    @Builder.Default
    private Integer resolvedQuarantineQty = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_sampling_method", length = 30)
    private QcSamplingMethod qcSamplingMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_result", length = 20)
    private QcResult qcResult;

    @Column(name = "qc_failure_reason", columnDefinition = "TEXT")
    private String qcFailureReason;

    @Column(name = "unit_cost", precision = 18, scale = 2)
    private BigDecimal unitCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_by")
    private User qcBy;
}
