package com.wms.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealerUpdateRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 1000)
    private String defaultDeliveryAddress;

    @Size(max = 100)
    private String region;
}
