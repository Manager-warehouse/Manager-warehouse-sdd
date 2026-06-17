package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReturnItemRequest {

    @NotNull(message = "PRODUCT_ID_REQUIRED")
    private Long productId;

    @NotNull(message = "EXPECTED_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.01", message = "EXPECTED_QUANTITY_MUST_BE_POSITIVE")
    private BigDecimal expectedQty;
}
