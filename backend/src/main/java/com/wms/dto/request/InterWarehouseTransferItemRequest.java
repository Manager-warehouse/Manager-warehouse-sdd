package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InterWarehouseTransferItemRequest(
        @NotNull Long productId,
        Long sourceLocationId,
        Long destinationLocationId,
        @NotNull @Positive BigDecimal plannedQty) {}
