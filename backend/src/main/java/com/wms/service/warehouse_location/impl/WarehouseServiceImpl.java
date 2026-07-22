package com.wms.service.warehouse_location.impl;


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
import com.wms.dto.request.WarehouseRequest;
import com.wms.dto.response.WarehouseResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.InventoryRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_location.WarehouseService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;


    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses(Boolean isActive, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Warehouse> list = (isActive != null) 
                ? warehouseRepository.findByIsActive(isActive)
                : warehouseRepository.findAll();

        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.CEO || user.getRole() == UserRole.WAREHOUSE_MANAGER) {
            return list.stream().map(mapper::toResponse).collect(Collectors.toList());
        } else {
            List<Long> assignedIds = userWarehouseAssignmentRepository.findWarehouseIdsByUserId(userId);
            return list.stream()
                    .filter(w -> assignedIds.contains(w.getId()))
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return mapper.toResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request, Long userId) {
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("DUPLICATE_WAREHOUSE_CODE");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
            if (manager.getRole() != UserRole.WAREHOUSE_MANAGER) {
                throw new IllegalArgumentException("USER_MUST_HAVE_WAREHOUSE_MANAGER_ROLE");
            }
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setPhone(request.getPhone());
        warehouse.setManager(manager);
        warehouse.setType(WarehouseType.valueOf(request.getType()));
        warehouse.setIsActive(true);
        warehouse.setCreatedBy(actor);
        warehouse.setUpdatedBy(actor);
        warehouse.setCreatedAt(OffsetDateTime.now());
        warehouse.setUpdatedAt(OffsetDateTime.now());

        Warehouse saved = warehouseRepository.save(warehouse);

        // Audit Log
        auditLogService.log(actor, AuditAction.CREATE, "Warehouse", saved.getId(), saved.getCode(), saved.getId(), null, toMap(saved));

        // If type is IN_TRANSIT, auto-create default Zone and Bin
        if (saved.getType() == WarehouseType.IN_TRANSIT) {
            createDefaultLocationsForInTransit(saved, actor);
        }

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request, Long userId) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        if (!warehouse.getCode().equals(request.getCode()) && warehouseRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new IllegalArgumentException("DUPLICATE_WAREHOUSE_CODE");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + request.getManagerId()));
            if (manager.getRole() != UserRole.WAREHOUSE_MANAGER) {
                throw new IllegalArgumentException("USER_MUST_HAVE_WAREHOUSE_MANAGER_ROLE");
            }
        }

        Map<String, Object> oldMap = toMap(warehouse);

        warehouse.setCode(request.getCode());
        warehouse.setName(request.getName());
        warehouse.setAddress(request.getAddress());
        warehouse.setPhone(request.getPhone());
        warehouse.setManager(manager);
        warehouse.setType(WarehouseType.valueOf(request.getType()));
        warehouse.setUpdatedBy(actor);
        warehouse.setUpdatedAt(OffsetDateTime.now());

        Warehouse saved = warehouseRepository.save(warehouse);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Warehouse", saved.getId(), saved.getCode(), saved.getId(), oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateWarehouse(Long id, Long userId) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        if (!warehouse.getIsActive()) {
            return;
        }

        // Check if there is active inventory in this warehouse
        boolean hasStock = inventoryRepository.existsByWarehouseIdAndTotalQtyGreaterThan(id, BigDecimal.ZERO);
        if (hasStock) {
            throw new IllegalArgumentException("WAREHOUSE_HAS_STOCK");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(warehouse);

        warehouse.setIsActive(false);
        warehouse.setUpdatedBy(actor);
        warehouse.setUpdatedAt(OffsetDateTime.now());

        Warehouse saved = warehouseRepository.save(warehouse);

        // Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "Warehouse", saved.getId(), saved.getCode(), saved.getId(), oldMap, toMap(saved));
    }

    @Override
    @Transactional
    public WarehouseResponse reactivateWarehouse(Long id, Long userId) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        if (warehouse.getIsActive()) {
            return mapper.toResponse(warehouse);
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(warehouse);

        warehouse.setIsActive(true);
        warehouse.setUpdatedBy(actor);
        warehouse.setUpdatedAt(OffsetDateTime.now());

        Warehouse saved = warehouseRepository.save(warehouse);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "Warehouse", saved.getId(), saved.getCode(), saved.getId(), oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    private void createDefaultLocationsForInTransit(Warehouse warehouse, User actor) {
        // 1. Create Zone Location
        WarehouseLocation zone = new WarehouseLocation();
        zone.setWarehouse(warehouse);
        zone.setCode(warehouse.getCode() + ".ZONE");
        zone.setType(LocationType.ZONE);
        zone.setParent(null);
        zone.setIsQuarantine(false);
        zone.setIsActive(true);
        zone.setCreatedBy(actor);
        zone.setUpdatedBy(actor);
        zone.setCreatedAt(OffsetDateTime.now());
        zone.setUpdatedAt(OffsetDateTime.now());
        zone.setCurrentVolumeM3(BigDecimal.ZERO);
        zone.setCurrentWeightKg(BigDecimal.ZERO);
        
        WarehouseLocation savedZone = locationRepository.save(zone);
        auditLogService.log(actor, AuditAction.CREATE, "WarehouseLocation", savedZone.getId(), savedZone.getCode(), warehouse.getId(), null, toMap(savedZone));

        // 2. Create Bin Location
        WarehouseLocation bin = new WarehouseLocation();
        bin.setWarehouse(warehouse);
        bin.setCode(warehouse.getCode() + ".ZONE.BIN");
        bin.setType(LocationType.BIN);
        bin.setParent(savedZone);
        bin.setIsQuarantine(false);
        bin.setIsActive(true);
        bin.setCreatedBy(actor);
        bin.setUpdatedBy(actor);
        bin.setCreatedAt(OffsetDateTime.now());
        bin.setUpdatedAt(OffsetDateTime.now());
        bin.setCurrentVolumeM3(BigDecimal.ZERO);
        bin.setCurrentWeightKg(BigDecimal.ZERO);

        WarehouseLocation savedBin = locationRepository.save(bin);
        auditLogService.log(actor, AuditAction.CREATE, "WarehouseLocation", savedBin.getId(), savedBin.getCode(), warehouse.getId(), null, toMap(savedBin));
    }

    private Map<String, Object> toMap(Warehouse w) {
        if (w == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", w.getId());
        map.put("code", w.getCode());
        map.put("name", w.getName());
        map.put("address", w.getAddress());
        map.put("phone", w.getPhone());
        map.put("type", w.getType() != null ? w.getType().name() : null);
        map.put("managerId", w.getManager() != null ? w.getManager().getId() : null);
        map.put("isActive", w.getIsActive());
        return map;
    }

    private Map<String, Object> toMap(WarehouseLocation loc) {
        if (loc == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", loc.getId());
        map.put("warehouseId", loc.getWarehouse() != null ? loc.getWarehouse().getId() : null);
        map.put("code", loc.getCode());
        map.put("type", loc.getType() != null ? loc.getType().name() : null);
        map.put("parentId", loc.getParent() != null ? loc.getParent().getId() : null);
        map.put("capacityM3", loc.getCapacityM3());
        map.put("capacityKg", loc.getCapacityKg());
        map.put("isQuarantine", loc.getIsQuarantine());
        map.put("isActive", loc.getIsActive());
        return map;
    }
}
