package com.wms.entity;

import com.wms.entity.enums.QcResult;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "receipt_items")
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

    @Column(name = "expected_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedQty;

    @Column(name = "actual_qty", precision = 10, scale = 2)
    private BigDecimal actualQty;

    @Column(name = "qc_passed_qty", precision = 10, scale = 2)
    private BigDecimal qcPassedQty;

    @Column(name = "qc_failed_qty", precision = 10, scale = 2)
    private BigDecimal qcFailedQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_result", length = 20)
    private QcResult qcResult;

    @Column(name = "qc_failure_reason", columnDefinition = "TEXT")
    private String qcFailureReason;

    @Column(name = "grade", length = 1)
    private String grade;

    @Column(name = "unit_cost", precision = 18, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;
}
