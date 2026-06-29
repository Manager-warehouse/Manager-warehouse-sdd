package com.wms.service;

import com.wms.dto.response.StockAlertResponse;
import org.springframework.data.domain.Page;

public interface StockAlertService {
    void checkAndTriggerAlert(Long warehouseId, Long productId);
    Page<StockAlertResponse> getLowStockAlerts(Long warehouseId, Long productId, Boolean isResolved, int page, int size, Long currentUserId);
}
