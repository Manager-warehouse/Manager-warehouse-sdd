package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequestItemRequest(
    @NotNull(message = "PRODUCT_ID_REQUIRED")
    Long productId,

    @NotNull(message = "REQUESTED_QTY_REQUIRED")
    @DecimalMin(value = "0.01", message = "REQUESTED_QTY_MUST_BE_POSITIVE")
    BigDecimal requestedQty
) {}
