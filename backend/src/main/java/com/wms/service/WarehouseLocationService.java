package com.wms.service;

import com.wms.dto.request.WarehouseLocationRequest;
import com.wms.dto.response.CapacityResponse;
import com.wms.dto.response.WarehouseLocationResponse;

import java.util.List;

public interface WarehouseLocationService {
    List<WarehouseLocationResponse> getAllLocations(Long warehouseId, String type, Boolean isQuarantine, Boolean isActive);
    WarehouseLocationResponse getLocationById(Long id);
    WarehouseLocationResponse createLocation(WarehouseLocationRequest request, Long userId);
    WarehouseLocationResponse updateLocation(Long id, WarehouseLocationRequest request, Long userId);
    void deactivateLocation(Long id, Long userId);
    CapacityResponse getCapacity(Long id);
}
