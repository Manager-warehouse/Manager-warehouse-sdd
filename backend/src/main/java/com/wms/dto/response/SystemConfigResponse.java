package com.wms.dto.response;


import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

/** DTO phản hồi cấu hình hệ thống hiện tại (Spec 001). */
@Data
@Builder
public class SystemConfigResponse {
    
    private Long id;
    private String configKey;
    private String configValue;
    private String description;
    private String updatedBy; // Full name of the user who updated it
    private OffsetDateTime updatedAt;
    
}
