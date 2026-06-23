package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseRequest {

    @NotBlank(message = "CODE_REQUIRED")
    @Size(max = 20, message = "CODE_TOO_LONG")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "INVALID_CODE_FORMAT")
    private String code;

    @NotBlank(message = "NAME_REQUIRED")
    @Size(max = 255, message = "NAME_TOO_LONG")
    private String name;

    private String address;

    @Size(max = 20, message = "PHONE_TOO_LONG")
    private String phone;

    private Long managerId;

    @NotBlank(message = "TYPE_REQUIRED")
    @Pattern(regexp = "^(PHYSICAL|IN_TRANSIT)$", message = "INVALID_WAREHOUSE_TYPE")
    private String type;
}
