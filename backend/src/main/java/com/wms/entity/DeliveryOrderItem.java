package com.wms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_order_items")
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

    @Column(name = "requested_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestedQty;

    @Column(name = "reserved_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservedQty;

    @Column(name = "issued_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal issuedQty;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picked_by")
    private User pickedBy;
}
