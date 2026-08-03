package com.wms.dto.request.stock_receiving;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReceiptItemUnitCostRequest {

    @NotNull(message = "RECEIPT_ITEM_ID_REQUIRED")
    private Long receiptItemId;

    @NotNull(message = "UNIT_COST_REQUIRED")
    @Positive(message = "UNIT_COST_INVALID")
    private BigDecimal unitCost;
}
