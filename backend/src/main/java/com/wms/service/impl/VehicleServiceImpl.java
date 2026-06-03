package com.wms.service.impl;

import com.wms.dto.request.VehicleRequest;
import com.wms.dto.response.VehicleResponse;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.enums.AuditAction;
import com.wms.enums.VehicleStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.UserRepository;
import com.wms.repository.VehicleRepository;
import com.wms.service.AuditLogService;
import com.wms.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles(String status, Boolean isActive) {
        List<Vehicle> list = vehicleRepository.findAll().stream()
                .filter(v -> status == null || v.getStatus().name().equals(status))
                .filter(v -> isActive == null || v.getIsActive().equals(isActive))
                .collect(Collectors.toList());
        return list.stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        return mapper.toResponse(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request, Long userId) {
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new IllegalArgumentException("DUPLICATE_PLATE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Vehicle vehicle = new Vehicle();
        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMaxWeightKg(request.getMaxWeightKg());
        vehicle.setMaxVolumeM3(request.getMaxVolumeM3());
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setIsActive(true);
        vehicle.setCreatedBy(actor);
        vehicle.setUpdatedBy(actor);
        vehicle.setCreatedAt(OffsetDateTime.now());
        vehicle.setUpdatedAt(OffsetDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        // Audit Log
        auditLogService.log(actor, AuditAction.CREATE, "Vehicle", saved.getId(), saved.getPlateNumber(), null, null, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (!vehicle.getPlateNumber().equals(request.getPlateNumber()) && vehicleRepository.existsByPlateNumberAndIdNot(request.getPlateNumber(), id)) {
            throw new IllegalArgumentException("DUPLICATE_PLATE_NUMBER");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(vehicle);

        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMaxWeightKg(request.getMaxWeightKg());
        vehicle.setMaxVolumeM3(request.getMaxVolumeM3());
        vehicle.setUpdatedBy(actor);
        vehicle.setUpdatedAt(OffsetDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Vehicle", saved.getId(), saved.getPlateNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse updateStatus(Long id, String status, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(vehicle);

        vehicle.setStatus(VehicleStatus.valueOf(status));
        vehicle.setUpdatedBy(actor);
        vehicle.setUpdatedAt(OffsetDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        // Audit Log
        auditLogService.log(actor, AuditAction.STATUS_CHANGE, "Vehicle", saved.getId(), saved.getPlateNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateVehicle(Long id, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (!vehicle.getIsActive()) {
            return;
        }

        if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            throw new IllegalArgumentException("VEHICLE_ON_TRIP");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(vehicle);

        vehicle.setIsActive(false);
        vehicle.setUpdatedBy(actor);
        vehicle.setUpdatedAt(OffsetDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        // Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Vehicle", saved.getId(), saved.getPlateNumber(), null, oldMap, toMap(saved));
    }

    private Map<String, Object> toMap(Vehicle v) {
        if (v == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", v.getId());
        map.put("plateNumber", v.getPlateNumber());
        map.put("vehicleType", v.getVehicleType());
        map.put("maxWeightKg", v.getMaxWeightKg());
        map.put("maxVolumeM3", v.getMaxVolumeM3());
        map.put("status", v.getStatus() != null ? v.getStatus().name() : null);
        map.put("isActive", v.getIsActive());
        return map;
    }
}
