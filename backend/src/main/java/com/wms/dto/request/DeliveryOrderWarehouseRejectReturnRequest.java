package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderWarehouseRejectReturnRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long allocationId;

    @NotNull
    private Long batchId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal returnedQty;

    @NotNull
    private Long sourceLocationId;

    @NotNull
    private Long originalLocationId;

    @NotNull
    private Long originalZoneId;

    @Size(max = 1000)
    private String reason;
}
