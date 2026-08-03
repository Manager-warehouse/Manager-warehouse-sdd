package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitDeliveryLegItemRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long productId;

    @NotNull
    private Long batchId;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal quantity;
}
