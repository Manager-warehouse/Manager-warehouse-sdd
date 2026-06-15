package com.wms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiptCreateItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private BigDecimal expectedQty;

    private BigDecimal unitCost;
}
