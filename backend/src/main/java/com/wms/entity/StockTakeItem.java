package com.wms.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_take_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTakeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_take_id", nullable = false)
    private StockTake stockTake;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "system_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal systemQty;

    // Nullable until the storekeeper records the count; the create→start→count flow
    // inserts items before any actual count exists (Spec 006).
    @Column(name = "actual_qty", precision = 10, scale = 2)
    private BigDecimal actualQty;

    @Column(name = "variance_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal varianceQty;

    @Column(name = "variance_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal varianceValue;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
