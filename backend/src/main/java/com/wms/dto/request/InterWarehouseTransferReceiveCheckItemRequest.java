package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record InterWarehouseTransferReceiveCheckItemRequest(
        @NotNull Long transferItemId,
        @NotNull @PositiveOrZero BigDecimal confirmedQty,
        @NotNull @PositiveOrZero BigDecimal qcPassedQty,
        @NotNull @PositiveOrZero BigDecimal qcFailedQty,
        Long destinationLocationId,
        String checkerNote,
        String qcFailureReason) {}
