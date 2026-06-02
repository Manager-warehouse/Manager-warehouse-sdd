package com.wms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "product_units")
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    @Column(name = "conversion_factor", nullable = false, precision = 10, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "is_base_unit", nullable = false)
    private Boolean isBaseUnit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
