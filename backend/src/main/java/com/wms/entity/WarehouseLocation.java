package com.wms.entity;

import com.wms.entity.enums.LocationType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "warehouse_locations")
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

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
