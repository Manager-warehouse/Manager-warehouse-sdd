package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderPickingPlanRequest {

    @Valid
    @NotEmpty
    private List<DeliveryOrderAllocationRequest> allocations;

    @Valid
    private List<DeliveryOrderReturnToBinRequest> returnToBinRecords;
}
