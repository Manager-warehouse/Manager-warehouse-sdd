package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnedGoodsPutawayPlanItemRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long batchId;

    private Long destinationLocationId;

    @DecimalMin(value = "0.00")
    private BigDecimal plannedQty;

    private Long failedDestinationLocationId;

    @DecimalMin(value = "0.00")
    private BigDecimal failedPlannedQty;
}
