package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderPickingPlanAdjustmentRequest {
    @NotBlank
    @Size(max = 1000)
    private String reason;
}
