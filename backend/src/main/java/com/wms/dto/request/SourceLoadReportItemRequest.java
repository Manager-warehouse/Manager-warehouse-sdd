package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SourceLoadReportItemRequest(
        @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED")
        Long transferItemId,

        @NotNull(message = "LOADED_QTY_REQUIRED")
        @DecimalMin(value = "0.00", message = "INVALID_LOADED_QTY")
        BigDecimal loadedQty,

        List<@Valid SourceLoadPickRequest> picks
) {
    public SourceLoadReportItemRequest(Long transferItemId, BigDecimal loadedQty) {
        this(transferItemId, loadedQty, null);
    }
}
