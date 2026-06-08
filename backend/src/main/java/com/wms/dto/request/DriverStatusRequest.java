package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusRequest {

    @NotBlank(message = "STATUS_REQUIRED")
    @Pattern(regexp = "^(AVAILABLE|ON_TRIP|UNAVAILABLE)$", message = "INVALID_DRIVER_STATUS")
    private String status;
}
