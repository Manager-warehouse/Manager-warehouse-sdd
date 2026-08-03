package com.wms.entity.order_fulfillment;


import com.wms.entity.access_control.User;
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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "do_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private WarehouseLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private WarehouseLocation zone;

    @Column(name = "requested_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQty;

    @Column(name = "reserved_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservedQty;

    @Column(name = "planned_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal plannedQty;

    @Column(name = "picked_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal pickedQty;

    @Column(name = "qc_pass_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal qcPassQty;

    @Column(name = "qc_fail_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal qcFailQty;

    @Column(name = "issued_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal issuedQty;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_cost", precision = 18, scale = 2)
    private BigDecimal unitCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picked_by")
    private User pickedBy;
}

