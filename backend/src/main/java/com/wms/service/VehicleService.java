package com.wms.service;

import com.wms.dto.request.VehicleRequest;
import com.wms.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    List<VehicleResponse> getAllVehicles(String status, Boolean isActive);
    VehicleResponse getVehicleById(Long id);
    VehicleResponse createVehicle(VehicleRequest request, Long userId);
    VehicleResponse updateVehicle(Long id, VehicleRequest request, Long userId);
    VehicleResponse updateStatus(Long id, String status, Long userId);
    void deactivateVehicle(Long id, Long userId);
    VehicleResponse reactivateVehicle(Long id, Long userId);
}
