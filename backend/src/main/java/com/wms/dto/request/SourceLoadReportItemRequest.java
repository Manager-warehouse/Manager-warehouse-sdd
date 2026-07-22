package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SourceLoadReportItemRequest(
        @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED")
        Long transferItemId,

        @NotNull(message = "LOADED_QTY_REQUIRED")
        @DecimalMin(value = "0.00", message = "INVALID_LOADED_QTY")
        BigDecimal loadedQty
) {}
