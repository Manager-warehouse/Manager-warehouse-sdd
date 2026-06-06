package com.wms.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String unit;
    private Integer unitPerPack;
    private String description;
    private String imageUrl;
    private BigDecimal weightKg;
    private BigDecimal volumeM3;
    private Boolean hasSerial;
    private Boolean hasExpiry;
    private Integer shelfLifeDays;
    private BigDecimal reorderPoint;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
