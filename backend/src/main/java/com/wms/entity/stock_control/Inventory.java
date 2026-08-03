package com.wms.entity.stock_control;


import com.wms.entity.product_catalog.Product;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "product_id", "batch_id", "location_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "total_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQty;

    @Column(name = "reserved_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal reservedQty;

    @Column(name = "cost_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal costPrice;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
