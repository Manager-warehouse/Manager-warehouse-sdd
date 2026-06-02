package com.wms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "unit_per_pack")
    private Integer unitPerPack;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "volume_m3", precision = 10, scale = 5)
    private BigDecimal volumeM3;

    @Column(name = "has_expiry", nullable = false)
    private Boolean hasExpiry;

    @Column(name = "has_serial", nullable = false)
    private Boolean hasSerial;

    @Column(name = "reorder_point", precision = 10, scale = 2)
    private BigDecimal reorderPoint;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
