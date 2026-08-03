package com.wms.dto.request;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record InterWarehouseTransferReceiveCheckItemRequest(
        @NotNull(message = "TRANSFER_ITEM_ID_REQUIRED") Long transferItemId,
        @NotNull(message = "CONFIRMED_QTY_REQUIRED")
        @PositiveOrZero(message = "CONFIRMED_QTY_MUST_NOT_BE_NEGATIVE")
        BigDecimal confirmedQty,
        @NotNull(message = "QC_PASSED_QTY_REQUIRED")
        @PositiveOrZero(message = "QC_QTY_MUST_NOT_BE_NEGATIVE")
        BigDecimal qcPassedQty,
        @NotNull(message = "QC_FAILED_QTY_REQUIRED")
        @PositiveOrZero(message = "QC_QTY_MUST_NOT_BE_NEGATIVE")
        BigDecimal qcFailedQty,
        Long destinationLocationId,
        String checkerNote,
        String qcFailureReason) {}
