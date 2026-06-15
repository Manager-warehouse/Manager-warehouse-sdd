package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptReceiveItemRequest {

    @NotNull
    private Long receiptItemId;

    @NotNull
    @PositiveOrZero
    private BigDecimal actualQty;
}
