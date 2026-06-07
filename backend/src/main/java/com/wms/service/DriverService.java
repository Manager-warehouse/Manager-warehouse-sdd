package com.wms.service;

import com.wms.dto.request.DriverRequest;
import com.wms.dto.response.DriverResponse;

import java.util.List;

public interface DriverService {
    List<DriverResponse> getAllDrivers(String status, Boolean isActive);
    DriverResponse getDriverById(Long id);
    DriverResponse createDriver(DriverRequest request, Long userId);
    DriverResponse updateDriver(Long id, DriverRequest request, Long userId);
    DriverResponse updateStatus(Long id, String status, Long userId);
    void deactivateDriver(Long id, Long userId);
}
