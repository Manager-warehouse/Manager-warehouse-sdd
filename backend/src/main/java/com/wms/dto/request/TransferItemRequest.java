package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferItemRequest(
        @NotNull Long productId,
        Long sourceLocationId,
        Long destinationLocationId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal plannedQty) {
}
