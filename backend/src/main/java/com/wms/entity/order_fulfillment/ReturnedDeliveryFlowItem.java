package com.wms.entity.order_fulfillment;

import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.warehouse_location.WarehouseLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "returned_delivery_flow_items")
@Getter
@Setter
public class ReturnedDeliveryFlowItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    private ReturnedDeliveryFlow flow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_item_id", nullable = false)
    private DeliveryOrderItem deliveryOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Column(name = "expected_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedQty;

    @Column(name = "actual_qty", precision = 10, scale = 2)
    private BigDecimal actualQty;

    @Column(name = "quality_pass_qty", precision = 10, scale = 2)
    private BigDecimal qualityPassQty;

    @Column(name = "quality_fail_qty", precision = 10, scale = 2)
    private BigDecimal qualityFailQty;

    @Column(name = "quality_failure_reason", columnDefinition = "TEXT")
    private String qualityFailureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private WarehouseLocation destinationLocation;

    @Column(name = "planned_qty", precision = 10, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "putaway_completed_qty", precision = 10, scale = 2)
    private BigDecimal putawayCompletedQty;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
