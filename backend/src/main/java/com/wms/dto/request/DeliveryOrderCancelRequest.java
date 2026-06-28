package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveryOrderCancelRequest {

    @NotBlank
    @Size(max = 500)
    private String cancelReason;
}
