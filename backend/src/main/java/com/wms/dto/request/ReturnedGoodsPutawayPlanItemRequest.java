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

    @NotNull
    private Long destinationLocationId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal plannedQty;
}
