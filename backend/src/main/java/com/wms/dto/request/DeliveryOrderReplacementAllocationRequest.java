package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderReplacementAllocationRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long failedInventoryId;

    @NotNull
    private Long failedBatchId;

    @NotNull
    private Long failedLocationId;

    @NotNull
    private Long replacementInventoryId;

    @NotNull
    private Long replacementBatchId;

    @NotNull
    private Long replacementLocationId;

    @NotNull
    private Long replacementZoneId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal quantity;

    @NotBlank
    private String reason;
}
