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
import com.wms.dto.request.WarehouseLocationRequest;
import com.wms.dto.response.CapacityResponse;
import com.wms.dto.response.WarehouseLocationResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.InventoryRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.warehouse_location.WarehouseLocationService;
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
public class WarehouseLocationServiceImpl implements WarehouseLocationService {

    private final WarehouseLocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final MasterDataMapper mapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseLocationResponse> getAllLocations(Long warehouseId, String type, Boolean isQuarantine, Boolean isStaging, Boolean isActive) {
        // Query filter logic
        List<WarehouseLocation> list = locationRepository.findAll().stream()
                .filter(loc -> warehouseId == null || loc.getWarehouse().getId().equals(warehouseId))
                .filter(loc -> type == null || loc.getType().name().equals(type))
                .filter(loc -> isQuarantine == null || loc.getIsQuarantine().equals(isQuarantine))
                .filter(loc -> isStaging == null || loc.getIsStaging().equals(isStaging))
                .filter(loc -> isActive == null || loc.getIsActive().equals(isActive))
                .collect(Collectors.toList());
        return list.stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseLocationResponse getLocationById(Long id) {
        WarehouseLocation loc = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse location not found with id: " + id));
        return mapper.toResponse(loc);
    }

    @Override
    @Transactional
    public WarehouseLocationResponse createLocation(WarehouseLocationRequest request, Long userId) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocationType type = LocationType.valueOf(request.getType());
        WarehouseLocation parent = null;

        if (type == LocationType.ZONE) {
            if (request.getParentId() != null) {
                throw new IllegalArgumentException("ZONE_CANNOT_HAVE_PARENT");
            }
        } else if (type == LocationType.BIN) {
            if (request.getParentId() == null) {
                throw new IllegalArgumentException("BIN_REQUIRES_PARENT_ZONE");
            }
            parent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent location not found with id: " + request.getParentId()));
            if (parent.getType() != LocationType.ZONE) {
                throw new IllegalArgumentException("PARENT_LOCATION_MUST_BE_ZONE");
            }
        }

        // Auto-prefix code if not already formatted
        String rawCode = request.getCode();
        String resolvedCode = rawCode;
        if (type == LocationType.ZONE && !rawCode.startsWith(warehouse.getCode() + ".")) {
            resolvedCode = warehouse.getCode() + "." + rawCode;
        } else if (type == LocationType.BIN && parent != null && !rawCode.startsWith(parent.getCode() + ".")) {
            resolvedCode = parent.getCode() + "." + rawCode;
        }

        if (locationRepository.existsByCode(resolvedCode)) {
            throw new IllegalArgumentException("DUPLICATE_LOCATION_CODE");
        }

        WarehouseLocation location = new WarehouseLocation();
        location.setWarehouse(warehouse);
        location.setCode(resolvedCode);
        location.setType(type);
        location.setParent(parent);
        location.setCapacityM3(request.getCapacityM3());
        location.setCapacityKg(request.getCapacityKg());
        location.setCurrentVolumeM3(BigDecimal.ZERO);
        location.setCurrentWeightKg(BigDecimal.ZERO);
        location.setIsQuarantine(request.getIsQuarantine() != null ? request.getIsQuarantine() : false);
        location.setIsStaging(request.getIsStaging() != null ? request.getIsStaging() : false);
        location.setIsActive(true);
        location.setCreatedBy(actor);
        location.setUpdatedBy(actor);
        location.setCreatedAt(OffsetDateTime.now());
        location.setUpdatedAt(OffsetDateTime.now());

        WarehouseLocation saved = locationRepository.save(location);

        // Audit Log
        auditLogService.log(actor, AuditAction.CREATE, "WarehouseLocation", saved.getId(), saved.getCode(), warehouse.getId(), null, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseLocationResponse updateLocation(Long id, WarehouseLocationRequest request, Long userId) {
        WarehouseLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse location not found with id: " + id));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId()));

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocationType type = LocationType.valueOf(request.getType());
        WarehouseLocation parent = null;

        if (type == LocationType.ZONE) {
            if (request.getParentId() != null) {
                throw new IllegalArgumentException("ZONE_CANNOT_HAVE_PARENT");
            }
        } else if (type == LocationType.BIN) {
            if (request.getParentId() == null) {
                throw new IllegalArgumentException("BIN_REQUIRES_PARENT_ZONE");
            }
            parent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent location not found with id: " + request.getParentId()));
            if (parent.getType() != LocationType.ZONE) {
                throw new IllegalArgumentException("PARENT_LOCATION_MUST_BE_ZONE");
            }
        }

        // Auto-prefix code if not already formatted
        String rawCode = request.getCode();
        String resolvedCode = rawCode;
        if (type == LocationType.ZONE && !rawCode.startsWith(warehouse.getCode() + ".")) {
            resolvedCode = warehouse.getCode() + "." + rawCode;
        } else if (type == LocationType.BIN && parent != null && !rawCode.startsWith(parent.getCode() + ".")) {
            resolvedCode = parent.getCode() + "." + rawCode;
        }

        if (!location.getCode().equals(resolvedCode) && locationRepository.existsByCodeAndIdNot(resolvedCode, id)) {
            throw new IllegalArgumentException("DUPLICATE_LOCATION_CODE");
        }

        // Capacity check if updating BIN capacity
        if (type == LocationType.BIN) {
            BigDecimal reqVol = request.getCapacityM3();
            BigDecimal reqWt = request.getCapacityKg();
            if (reqVol != null && reqVol.compareTo(location.getCurrentVolumeM3()) < 0) {
                throw new IllegalArgumentException("BIN_OVER_CAPACITY");
            }
            if (reqWt != null && reqWt.compareTo(location.getCurrentWeightKg()) < 0) {
                throw new IllegalArgumentException("BIN_OVER_CAPACITY");
            }
        }

        Map<String, Object> oldMap = toMap(location);

        location.setWarehouse(warehouse);
        location.setCode(resolvedCode);
        location.setType(type);
        location.setParent(parent);
        location.setCapacityM3(request.getCapacityM3());
        location.setCapacityKg(request.getCapacityKg());
        location.setIsQuarantine(request.getIsQuarantine() != null ? request.getIsQuarantine() : location.getIsQuarantine());
        location.setIsStaging(request.getIsStaging() != null ? request.getIsStaging() : location.getIsStaging());
        location.setUpdatedBy(actor);
        location.setUpdatedAt(OffsetDateTime.now());

        WarehouseLocation saved = locationRepository.save(location);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "WarehouseLocation", saved.getId(), saved.getCode(), warehouse.getId(), oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateLocation(Long id, Long userId) {
        WarehouseLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse location not found with id: " + id));

        if (!location.getIsActive()) {
            return;
        }

        // Check if there is active inventory in this location
        boolean hasStock = inventoryRepository.existsByLocationIdAndTotalQtyGreaterThan(id, BigDecimal.ZERO);
        if (hasStock) {
            throw new IllegalArgumentException("LOCATION_HAS_STOCK");
        }

        // If zone, check if there are active bins under it
        if (location.getType() == LocationType.ZONE) {
            long activeBins = locationRepository.countByParentIdAndIsActiveTrue(id);
            if (activeBins > 0) {
                throw new IllegalArgumentException("ZONE_HAS_ACTIVE_BINS");
            }
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(location);

        location.setIsActive(false);
        location.setUpdatedBy(actor);
        location.setUpdatedAt(OffsetDateTime.now());

        WarehouseLocation saved = locationRepository.save(location);

        // Audit Log
        auditLogService.log(actor, AuditAction.SOFT_DELETE, "WarehouseLocation", saved.getId(), saved.getCode(), location.getWarehouse().getId(), oldMap, toMap(saved));
    }

    @Override
    @Transactional
    public WarehouseLocationResponse reactivateLocation(Long id, Long userId) {
        WarehouseLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse location not found with id: " + id));

        if (location.getIsActive()) {
            return mapper.toResponse(location);
        }

        // If bin, verify parent zone is active
        if (location.getType() == LocationType.BIN && location.getParent() != null && !location.getParent().getIsActive()) {
            throw new IllegalArgumentException("PARENT_ZONE_INACTIVE");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> oldMap = toMap(location);

        location.setIsActive(true);
        location.setUpdatedBy(actor);
        location.setUpdatedAt(OffsetDateTime.now());

        WarehouseLocation saved = locationRepository.save(location);

        // Audit Log
        auditLogService.log(actor, AuditAction.UPDATE, "WarehouseLocation", saved.getId(), saved.getCode(), location.getWarehouse().getId(), oldMap, toMap(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CapacityResponse getCapacity(Long id) {
        WarehouseLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse location not found with id: " + id));

        if (location.getType() == LocationType.BIN) {
            BigDecimal capM3 = location.getCapacityM3();
            BigDecimal capKg = location.getCapacityKg();
            BigDecimal curVol = location.getCurrentVolumeM3();
            BigDecimal curWt = location.getCurrentWeightKg();

            BigDecimal availVol = capM3 != null ? capM3.subtract(curVol) : null;
            BigDecimal availWt = capKg != null ? capKg.subtract(curWt) : null;

            return new CapacityResponse(capM3, capKg, curVol, curWt, availVol, availWt);
        } else {
            // Zone capacity: sum of child bins capacity and occupancy
            List<WarehouseLocation> bins = locationRepository.findByParentId(id);
            BigDecimal capM3 = BigDecimal.ZERO;
            BigDecimal capKg = BigDecimal.ZERO;
            BigDecimal curVol = BigDecimal.ZERO;
            BigDecimal curWt = BigDecimal.ZERO;

            for (WarehouseLocation bin : bins) {
                if (bin.getIsActive()) {
                    if (bin.getCapacityM3() != null) capM3 = capM3.add(bin.getCapacityM3());
                    if (bin.getCapacityKg() != null) capKg = capKg.add(bin.getCapacityKg());
                    curVol = curVol.add(bin.getCurrentVolumeM3());
                    curWt = curWt.add(bin.getCurrentWeightKg());
                }
            }

            BigDecimal availVol = capM3.compareTo(BigDecimal.ZERO) > 0 ? capM3.subtract(curVol) : null;
            BigDecimal availWt = capKg.compareTo(BigDecimal.ZERO) > 0 ? capKg.subtract(curWt) : null;

            return new CapacityResponse(
                    capM3.compareTo(BigDecimal.ZERO) > 0 ? capM3 : null,
                    capKg.compareTo(BigDecimal.ZERO) > 0 ? capKg : null,
                    curVol, curWt, availVol, availWt
            );
        }
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
        map.put("isStaging", loc.getIsStaging());
        map.put("isActive", loc.getIsActive());
        return map;
    }
}
