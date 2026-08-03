package com.wms.entity.warehouse_location;


import com.wms.entity.access_control.User;
import com.wms.enums.warehouse_location.LocationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouse_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private LocationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private WarehouseLocation parent;

    @Column(name = "capacity_m3", precision = 10, scale = 3)
    private BigDecimal capacityM3;

    @Column(name = "capacity_kg", precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "current_volume_m3", nullable = false, precision = 10, scale = 3)
    private BigDecimal currentVolumeM3;

    @Column(name = "current_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "is_quarantine", nullable = false)
    private Boolean isQuarantine;

    @Builder.Default
    @Column(name = "is_staging", nullable = false)
    private Boolean isStaging = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    // References the stock_takes.id that holds the lock; plain FK value to avoid circular dependency
    @Column(name = "locked_by_stock_take_id")
    private Long lockedByStockTakeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
