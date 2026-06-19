package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderAllocationRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long inventoryId;

    @NotNull
    private Long batchId;

    @NotNull
    private Long locationId;

    @NotNull
    private Long zoneId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal plannedQty;
}
