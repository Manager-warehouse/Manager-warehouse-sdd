package com.wms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TripDeliveryOrderRequest {

    @NotNull
    private Long doId;

    @NotNull
    @Min(1)
    private Integer stopOrder;
}
