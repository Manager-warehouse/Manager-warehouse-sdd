package com.wms.service;

import com.wms.dto.request.WarehouseRequest;
import com.wms.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    List<WarehouseResponse> getAllWarehouses(Boolean isActive, Long userId);
    WarehouseResponse getWarehouseById(Long id);
    WarehouseResponse createWarehouse(WarehouseRequest request, Long userId);
    WarehouseResponse updateWarehouse(Long id, WarehouseRequest request, Long userId);
    void deactivateWarehouse(Long id, Long userId);
    WarehouseResponse reactivateWarehouse(Long id, Long userId);
}
