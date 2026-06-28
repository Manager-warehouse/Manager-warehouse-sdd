package com.wms.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderPickQcRowRequest {

    @NotNull
    private Long doItemId;

    @NotNull
    private Long allocationId;

    @NotNull
    private Long batchId;

    @NotNull
    private Long locationId;

    @NotNull
    private Long zoneId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal pickedQty;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal qcPassQty;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal qcFailQty;

    @Size(max = 1000)
    private String qcFailReason;

    private Long stagingLocationId;

    private Long quarantineLocationId;

    @Size(max = 1000)
    private String notes;
}
