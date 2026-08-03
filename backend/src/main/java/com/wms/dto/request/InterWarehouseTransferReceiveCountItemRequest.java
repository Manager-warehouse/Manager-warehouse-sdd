package com.wms.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record InterWarehouseTransferReceiveCountItemRequest(
        @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED") Long transferItemId,
        @NotNull(message = "RECEIVED_QTY_REQUIRED")
        @PositiveOrZero(message = "RECEIVED_QTY_MUST_NOT_BE_NEGATIVE")
        BigDecimal receivedQty,
        String issueReason) {}
