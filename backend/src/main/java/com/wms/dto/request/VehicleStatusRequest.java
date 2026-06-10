package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleStatusRequest {

    @NotBlank(message = "STATUS_REQUIRED")
    @Pattern(regexp = "^(AVAILABLE|ON_TRIP|MAINTENANCE)$", message = "INVALID_VEHICLE_STATUS")
    private String status;
}
