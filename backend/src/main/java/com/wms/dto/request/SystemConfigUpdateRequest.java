package com.wms.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO cập nhật cấu hình tham số hệ thống (Spec 001). */
@Data
public class SystemConfigUpdateRequest {

    @NotBlank(message = "config_value is required")
    private String configValue;

}
