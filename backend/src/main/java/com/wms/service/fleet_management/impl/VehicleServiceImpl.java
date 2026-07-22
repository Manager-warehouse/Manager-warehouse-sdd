package com.wms.service.fleet_management.impl;


import com.wms.entity.access_control.*;
import com.wms.entity.audit_trail.*;
import com.wms.entity.billing_payment.*;
import com.wms.entity.dealer_management.*;
import com.wms.entity.document_numbering.*;
import com.wms.entity.driver_management.*;
import com.wms.entity.fleet_management.*;
import com.wms.entity.notification_delivery.*;
import com.wms.entity.order_fulfillment.*;
import com.wms.entity.price_management.*;
import com.wms.entity.product_catalog.*;
import com.wms.entity.stock_control.*;
import com.wms.entity.stock_counting.*;
import com.wms.entity.stock_receiving.*;
import com.wms.entity.supplier_management.*;
import com.wms.entity.user_configuration.*;
import com.wms.entity.warehouse_location.*;
import com.wms.entity.warehouse_transfer.*;
import com.wms.enums.access_control.*;
import com.wms.enums.audit_trail.*;
import com.wms.enums.billing_payment.*;
import com.wms.enums.dealer_management.*;
import com.wms.enums.driver_management.*;
import com.wms.enums.fleet_management.*;
import com.wms.enums.notification_delivery.*;
import com.wms.enums.order_fulfillment.*;
import com.wms.enums.price_management.*;
import com.wms.enums.stock_control.*;
import com.wms.enums.stock_counting.*;
import com.wms.enums.stock_receiving.*;
import com.wms.enums.supplier_management.*;
import com.wms.enums.user_configuration.*;
import com.wms.enums.warehouse_location.*;
import com.wms.enums.warehouse_transfer.*;
import com.wms.dto.request.VehicleRequest;
import com.wms.dto.response.VehicleResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.UserRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.fleet_management.VehicleService;
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
    private final WarehouseRepository warehouseRepository;
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
        try {
            // Kiểm tra trùng lặp plate number
            if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
                throw new IllegalArgumentException("DUPLICATE_PLATE_NUMBER: Vehicle with plate number '" + 
                    request.getPlateNumber() + "' already exists");
            }

            // Kiểm tra user tồn tại
            User actor = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            // Kiểm tra warehouse tồn tại
            Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNumber(request.getPlateNumber());
            vehicle.setVehicleType(request.getVehicleType());
            vehicle.setMaxWeightKg(request.getMaxWeightKg());
            vehicle.setMaxVolumeM3(request.getMaxVolumeM3());
            vehicle.setWarehouse(warehouse);
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
        } catch (Exception e) {
            // Log chi tiết lỗi để debug
            System.err.println("Error creating vehicle: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        Map<String, Object> oldMap = toMap(vehicle);

        vehicle.setWarehouse(warehouse);
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

    @Override
    @Transactional
    public VehicleResponse reactivateVehicle(Long id, Long userId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (vehicle.getIsActive()) {
            return mapper.toResponse(vehicle);
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(vehicle);

        vehicle.setIsActive(true);
        vehicle.setUpdatedBy(actor);
        vehicle.setUpdatedAt(OffsetDateTime.now());

        Vehicle saved = vehicleRepository.save(vehicle);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Vehicle", saved.getId(), saved.getPlateNumber(), null, oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    private Map<String, Object> toMap(Vehicle v) {
        if (v == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", v.getId());
        map.put("warehouseId", v.getWarehouse() != null ? v.getWarehouse().getId() : null);
        map.put("plateNumber", v.getPlateNumber());
        map.put("vehicleType", v.getVehicleType());
        map.put("maxWeightKg", v.getMaxWeightKg());
        map.put("maxVolumeM3", v.getMaxVolumeM3());
        map.put("status", v.getStatus() != null ? v.getStatus().name() : null);
        map.put("isActive", v.getIsActive());
        return map;
    }
}
