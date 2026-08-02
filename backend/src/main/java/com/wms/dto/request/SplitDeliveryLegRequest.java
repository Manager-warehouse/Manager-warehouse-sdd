package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitDeliveryLegRequest {

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long driverId;

    @NotNull
    @Positive
    private Integer stopOrder;

    @Valid
    @NotEmpty
    private List<SplitDeliveryLegItemRequest> items;
}
