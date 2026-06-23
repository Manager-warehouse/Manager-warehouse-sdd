package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderReturnToBinRequest {

    @NotNull
    private Long allocationId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal returnedQty;

    @NotNull
    private Long sourceLocationId;

    private String reason;
}
