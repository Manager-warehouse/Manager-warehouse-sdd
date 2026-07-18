package com.wms.service;

import com.wms.dto.request.SystemConfigUpdateRequest;
import com.wms.dto.response.SystemConfigResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SystemConfigService {

    List<SystemConfigResponse> getAllConfigs();

    SystemConfigResponse updateConfig(String configKey, SystemConfigUpdateRequest request, Long adminUserId);

    // Single typed-read path for every service that needs a parsed config value, so a
    // config key is never read/parsed with divergent logic in different call sites.
    int getIntValue(String configKey, int defaultValue);

    BigDecimal getDecimalValue(String configKey, BigDecimal defaultValue);
}
