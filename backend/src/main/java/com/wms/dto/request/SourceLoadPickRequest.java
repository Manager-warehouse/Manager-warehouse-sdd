package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SourceLoadPickRequest(
        @NotNull(message = "INVENTORY_ID_REQUIRED")
        Long inventoryId,

        @NotNull(message = "LOCATION_ID_REQUIRED")
        Long locationId,

        @NotNull(message = "PICK_QTY_REQUIRED")
        @DecimalMin(value = "0.01", message = "INVALID_PICK_QTY")
        BigDecimal quantity
) {}
