package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderItemCreateRequest {

    @NotNull
    private Long productId;

    private Long batchId;

    private Long locationId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal requestedQty;

    private BigDecimal unitPrice;

}
