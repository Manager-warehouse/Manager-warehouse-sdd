package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WrongSkuItemRequest(
    @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED")
    Long transferItemId,

    @NotNull(message = "EXPECTED_PRODUCT_ID_REQUIRED")
    Long expectedProductId,

    @NotNull(message = "ACTUAL_PRODUCT_ID_REQUIRED")
    Long actualProductId,

    @NotNull(message = "AFFECTED_QTY_REQUIRED")
    @Positive(message = "AFFECTED_QTY_MUST_BE_POSITIVE")
    BigDecimal affectedQty,

    @NotBlank(message = "WRONG_SKU_REASON_REQUIRED")
    String reason,

    String photoRef
) {}
