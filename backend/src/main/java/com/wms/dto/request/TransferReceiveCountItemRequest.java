package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferReceiveCountItemRequest(
        @NotNull Long transferItemId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal receivedQty,
        String issueReason) {
}
