package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VehicleRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotBlank(message = "PLATE_NUMBER_REQUIRED")
    @Size(max = 20, message = "PLATE_NUMBER_TOO_LONG")
    private String plateNumber;

    @NotBlank(message = "VEHICLE_TYPE_REQUIRED")
    @Size(max = 100, message = "VEHICLE_TYPE_TOO_LONG")
    private String vehicleType;

    @NotNull(message = "MAX_WEIGHT_REQUIRED")
    @DecimalMin(value = "0.01", message = "MAX_WEIGHT_MUST_BE_POSITIVE")
    private BigDecimal maxWeightKg;

    @DecimalMin(value = "0.001", message = "MAX_VOLUME_MUST_BE_POSITIVE")
    private BigDecimal maxVolumeM3;

    @NotNull(message = "WAREHOUSE_REQUIRED")
    private Long warehouseId;
}
