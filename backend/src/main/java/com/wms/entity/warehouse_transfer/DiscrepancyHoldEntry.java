package com.wms.entity.warehouse_transfer;


import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "discrepancy_hold_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyHoldEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private DiscrepancyIncident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "hold_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal holdQty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_location_id")
    private WarehouseLocation holdLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
