package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DriverRequest {

    @NotNull(message = "WAREHOUSE_ID_REQUIRED")
    private Long warehouseId;

    @NotNull(message = "USER_ID_REQUIRED")
    private Long userId;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    @Size(max = 255, message = "FULL_NAME_TOO_LONG")
    private String fullName;

    @Size(max = 20, message = "PHONE_TOO_LONG")
    private String phone;

    @NotBlank(message = "LICENSE_NUMBER_REQUIRED")
    @Size(max = 50, message = "LICENSE_NUMBER_TOO_LONG")
    private String licenseNumber;

    @NotNull(message = "LICENSE_EXPIRY_REQUIRED")
    private LocalDate licenseExpiry;
}
