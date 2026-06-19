package com.wms.service;

import com.wms.dto.request.DriverRequest;
import com.wms.dto.response.DriverResponse;
import com.wms.dto.response.UserResponse;

import java.util.List;

public interface DriverService {
    List<UserResponse> getDriverUserCandidates(Long actorId);
    List<DriverResponse> getAllDrivers(String status, Boolean isActive, Long actorId);
    DriverResponse getDriverById(Long id, Long actorId);
    DriverResponse createDriver(DriverRequest request, Long userId);
    DriverResponse updateDriver(Long id, DriverRequest request, Long userId);
    DriverResponse updateStatus(Long id, String status, Long userId);
    void deactivateDriver(Long id, Long userId);
    DriverResponse reactivateDriver(Long id, Long userId);
}
