package com.wms.service;

import com.wms.dto.response.CeoDashboardResponse;
import com.wms.dto.response.InventoryValuationResponse;

public interface ReportService {
    CeoDashboardResponse getCeoDashboard(Long currentUserId);
    InventoryValuationResponse getInventoryValuation(Long warehouseId, Long currentUserId);
}
