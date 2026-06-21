package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record InterWarehouseTransferReceiveCountItemRequest(
        @NotNull Long transferItemId,
        @NotNull @PositiveOrZero BigDecimal receivedQty,
        String issueReason) {}
