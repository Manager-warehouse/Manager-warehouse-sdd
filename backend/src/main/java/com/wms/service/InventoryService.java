package com.wms.service;

import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;

public interface InventoryService {
    InventoryAvailabilityResponse getAvailability(Long warehouseId, Long productId);

    WarehouseStockOverviewResponse getOverview(Long warehouseId);
}
