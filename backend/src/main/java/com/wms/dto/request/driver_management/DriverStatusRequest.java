package com.wms.dto.request.driver_management;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusRequest {

    @NotBlank(message = "STATUS_REQUIRED")
    @Pattern(regexp = "^(AVAILABLE|UNAVAILABLE)$", message = "INVALID_DRIVER_STATUS")
    private String status;
}
