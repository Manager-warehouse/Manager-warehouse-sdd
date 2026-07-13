package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WrongSkuItemRequest(
    @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED")
    Long transferItemId,

    @NotNull(message = "EXPECTED_PRODUCT_ID_REQUIRED")
    Long expectedProductId,

    @NotNull(message = "ACTUAL_PRODUCT_ID_REQUIRED")
    Long actualProductId,

    @NotNull(message = "AFFECTED_QTY_REQUIRED")
    BigDecimal affectedQty,

    String reason,

    String photoRef
) {}
