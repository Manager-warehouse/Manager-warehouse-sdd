package com.wms.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class WarehouseLocationResponse {
    private Long id;
    private Long warehouseId;
    private String code;
    private String type;
    private Long parentId;
    private String parentCode;
    private BigDecimal capacityM3;
    private BigDecimal capacityKg;
    private BigDecimal currentVolumeM3;
    private BigDecimal currentWeightKg;
    private Boolean isQuarantine;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
