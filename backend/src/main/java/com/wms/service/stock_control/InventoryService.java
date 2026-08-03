package com.wms.service.stock_control;


import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.ProductAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import java.util.List;

public interface InventoryService {
    InventoryAvailabilityResponse getAvailability(Long warehouseId, Long productId);

    List<ProductAvailabilityResponse> getAllAvailability(Long warehouseId);

    WarehouseStockOverviewResponse getOverview(Long warehouseId);
}
