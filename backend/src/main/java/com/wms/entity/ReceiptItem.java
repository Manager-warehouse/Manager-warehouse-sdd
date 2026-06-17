package com.wms.entity;

import lombok.*;
import com.wms.enums.QcResult;
import com.wms.enums.QcSamplingMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;

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

    @Column(name = "sample_qty")
    private Integer sampleQty;

    @Column(name = "sample_passed_qty")
    private Integer samplePassedQty;

    @Column(name = "sample_failed_qty")
    private Integer sampleFailedQty;

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
