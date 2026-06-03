package com.wms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemConfigUpdateRequest {

    @NotBlank(message = "config_value is required")
    private String configValue;

}
