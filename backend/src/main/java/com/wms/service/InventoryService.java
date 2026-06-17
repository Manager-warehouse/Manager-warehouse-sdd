package com.wms.service;

import com.wms.dto.response.InventoryAvailabilityResponse;

public interface InventoryService {
    InventoryAvailabilityResponse getAvailability(Long warehouseId, Long productId);
}
