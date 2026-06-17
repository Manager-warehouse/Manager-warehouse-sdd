package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferReceiveCheckItemRequest(
        @NotNull Long transferItemId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal confirmedQty,
        @NotNull @DecimalMin(value = "0.00") BigDecimal qcPassedQty,
        @NotNull @DecimalMin(value = "0.00") BigDecimal qcFailedQty,
        Long destinationLocationId,
        String checkerNote,
        String qcFailureReason) {
}
