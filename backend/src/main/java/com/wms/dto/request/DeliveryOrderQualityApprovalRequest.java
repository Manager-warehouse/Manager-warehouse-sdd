package com.wms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderQualityApprovalRequest {

    @Size(max = 1000)
    private String notes;
}
