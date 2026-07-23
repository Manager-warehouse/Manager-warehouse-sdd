package com.wms.mapper;
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

import com.wms.dto.response.*;
import com.wms.dto.response.driver_management.DriverResponse;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {

    public WarehouseResponse toResponse(Warehouse entity) {
        if (entity == null) {
            return null;
        }
        WarehouseResponse response = new WarehouseResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setAddress(entity.getAddress());
        response.setPhone(entity.getPhone());
        response.setType(entity.getType() != null ? entity.getType().name() : null);
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getManager() != null) {
            response.setManagerId(entity.getManager().getId());
            response.setManagerName(entity.getManager().getFullName());
        }
        return response;
    }

    public WarehouseLocationResponse toResponse(WarehouseLocation entity) {
        if (entity == null) {
            return null;
        }
        WarehouseLocationResponse response = new WarehouseLocationResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setType(entity.getType() != null ? entity.getType().name() : null);
        response.setCapacityM3(entity.getCapacityM3());
        response.setCapacityKg(entity.getCapacityKg());
        response.setCurrentVolumeM3(entity.getCurrentVolumeM3());
        response.setCurrentWeightKg(entity.getCurrentWeightKg());
        response.setIsQuarantine(entity.getIsQuarantine());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getWarehouse() != null) {
            response.setWarehouseId(entity.getWarehouse().getId());
        }
        if (entity.getParent() != null) {
            response.setParentId(entity.getParent().getId());
            response.setParentCode(entity.getParent().getCode());
        }
        return response;
    }

    public VehicleResponse toResponse(Vehicle entity) {
        if (entity == null) {
            return null;
        }
        VehicleResponse response = new VehicleResponse();
        response.setId(entity.getId());
        response.setPlateNumber(entity.getPlateNumber());
        response.setVehicleType(entity.getVehicleType());
        response.setMaxWeightKg(entity.getMaxWeightKg());
        response.setMaxVolumeM3(entity.getMaxVolumeM3());
        if (entity.getWarehouse() != null) {
            response.setWarehouseId(entity.getWarehouse().getId());
            response.setWarehouseCode(entity.getWarehouse().getCode());
            response.setWarehouseName(entity.getWarehouse().getName());
        }
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public DriverResponse toResponse(Driver entity) {
        if (entity == null) {
            return null;
        }
        DriverResponse response = new DriverResponse();
        response.setId(entity.getId());
        response.setFullName(entity.getFullName());
        response.setPhone(entity.getPhone());
        response.setLicenseNumber(entity.getLicenseNumber());
        response.setLicenseExpiry(entity.getLicenseExpiry());
        if (entity.getWarehouse() != null) {
            response.setWarehouseId(entity.getWarehouse().getId());
        }
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getUser() != null) {
            response.setUserId(entity.getUser().getId());
        }
        return response;
    }
}
