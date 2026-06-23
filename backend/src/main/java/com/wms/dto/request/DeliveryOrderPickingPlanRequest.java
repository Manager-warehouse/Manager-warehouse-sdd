package com.wms.dto.request;

import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderPickingPlanRequest {

    @Valid
    private List<DeliveryOrderAllocationRequest> allocations;

    @Valid
    private List<DeliveryOrderReturnToBinRequest> returnToBinRecords;
}
