package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailDeliveryRequest {

    @NotBlank
    @Size(max = 1000)
    private String failureReason;

    @Size(max = 1000)
    private String notes;
}
