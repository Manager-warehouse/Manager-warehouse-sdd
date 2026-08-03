package com.wms.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InterWarehouseTransferItemRequest(
        @NotNull(message = "PRODUCT_ID_REQUIRED") Long productId,
        Long sourceLocationId,
        Long destinationLocationId,
        @NotNull(message = "PLANNED_QTY_REQUIRED")
        @Positive(message = "PLANNED_QTY_MUST_BE_POSITIVE")
        BigDecimal plannedQty) {}
