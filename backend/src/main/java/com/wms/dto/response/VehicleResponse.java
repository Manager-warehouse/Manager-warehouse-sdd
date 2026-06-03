package com.wms.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
public class VehicleResponse {
    private Long id;
    private String plateNumber;
    private String vehicleType;
    private BigDecimal maxWeightKg;
    private BigDecimal maxVolumeM3;
    private String status;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
