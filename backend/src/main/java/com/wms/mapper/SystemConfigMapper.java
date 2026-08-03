package com.wms.mapper;


import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.user_configuration.SystemConfig;
import org.springframework.stereotype.Component;

/** Mapper chuyển đổi giữa SystemConfig entity và DTO request/response (Spec 001). */
@Component
public class SystemConfigMapper {

    public SystemConfigResponse toResponse(SystemConfig entity) {
        if (entity == null) {
            return null;
        }
        
        return SystemConfigResponse.builder()
                .id(entity.getId())
                .configKey(entity.getConfigKey())
                .configValue(entity.getConfigValue())
                .description(entity.getDescription())
                .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getFullName() : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
