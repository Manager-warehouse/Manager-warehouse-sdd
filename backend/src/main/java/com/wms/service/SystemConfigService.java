package com.wms.service;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;

import java.util.List;

public interface SystemConfigService {
    
    List<SystemConfigResponse> getAllConfigs();
    
    SystemConfigResponse updateConfig(String configKey, SystemConfigUpdateRequest request, Long adminUserId);
}
