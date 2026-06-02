package com.wms.entity;

import com.wms.enums.AlertType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_alerts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id", "alert_type", "is_resolved"}))
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentQty;

    @Column(name = "reorder_point", nullable = false, precision = 10, scale = 2)
    private BigDecimal reorderPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
