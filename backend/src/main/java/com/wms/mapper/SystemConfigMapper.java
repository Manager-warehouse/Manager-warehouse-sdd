package com.wms.mapper;

import com.wms.dto.response.SystemConfigResponse;
import com.wms.entity.SystemConfig;
import org.springframework.stereotype.Component;

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
