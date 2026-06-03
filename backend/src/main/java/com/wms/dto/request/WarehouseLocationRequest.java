package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WarehouseLocationRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotBlank(message = "CODE_REQUIRED")
    @Size(max = 50, message = "CODE_TOO_LONG")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "INVALID_CODE_FORMAT")
    private String code;

    @NotBlank(message = "TYPE_REQUIRED")
    @Pattern(regexp = "^(ZONE|BIN)$", message = "INVALID_LOCATION_TYPE")
    private String type;

    private Long parentId;

    @DecimalMin(value = "0.001", message = "CAPACITY_MUST_BE_POSITIVE")
    private BigDecimal capacityM3;

    @DecimalMin(value = "0.01", message = "CAPACITY_MUST_BE_POSITIVE")
    private BigDecimal capacityKg;

    private Boolean isQuarantine;
}
