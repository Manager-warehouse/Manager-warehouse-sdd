package com.wms.entity.stock_receiving;


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
import lombok.*;
import com.wms.enums.stock_receiving.QcResult;
import com.wms.enums.stock_receiving.QcSamplingMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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

    @Column(name = "over_received_qty", nullable = false)
    @Builder.Default
    private Integer overReceivedQty = 0;

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
