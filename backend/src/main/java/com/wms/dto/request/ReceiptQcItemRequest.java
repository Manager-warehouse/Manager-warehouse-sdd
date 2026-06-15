package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptQcItemRequest {

    @NotNull
    private Long receiptItemId;

    @NotNull
    @PositiveOrZero
    private BigDecimal sampleQty;

    @NotNull
    @PositiveOrZero
    private BigDecimal samplePassedQty;

    @NotNull
    @PositiveOrZero
    private BigDecimal sampleFailedQty;

    private String qcFailureReason;
}
