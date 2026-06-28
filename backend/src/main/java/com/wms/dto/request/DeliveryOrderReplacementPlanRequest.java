package com.wms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderReplacementPlanRequest {

    @Valid
    @NotEmpty
    private List<DeliveryOrderReplacementAllocationRequest> replacements;
}
