package com.wms.service;


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
import com.wms.service.user_configuration.*;
import com.wms.service.user_configuration.impl.*;
import com.wms.service.audit_trail.*;
import com.wms.service.access_control.*;
import com.wms.service.dealer_management.*;
import com.wms.service.dealer_management.impl.*;
import com.wms.service.billing_payment.*;
import com.wms.service.billing_payment.impl.*;
import com.wms.service.stock_receiving.*;
import com.wms.service.stock_control.*;
import com.wms.service.stock_control.impl.*;
import com.wms.service.notification_delivery.*;
import com.wms.service.notification_delivery.impl.*;
import com.wms.service.order_fulfillment.*;
import com.wms.service.order_fulfillment.impl.*;
import com.wms.service.price_management.*;
import com.wms.service.price_management.impl.*;
import com.wms.service.reporting_alerting.*;
import com.wms.service.reporting_alerting.impl.*;
import com.wms.service.return_disposal.*;
import com.wms.service.stock_counting.*;
import com.wms.service.fleet_management.*;
import com.wms.service.fleet_management.impl.*;
import com.wms.service.warehouse_location.*;
import com.wms.service.warehouse_location.impl.*;

import com.wms.dto.request.WarehouseLocationRequest;
import com.wms.dto.response.CapacityResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.access_control.UserRole;
import com.wms.repository.InventoryRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.warehouse_location.impl.WarehouseLocationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WarehouseLocationServiceTest {

    @Mock
    private WarehouseLocationRepository locationRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private com.wms.mapper.MasterDataMapper mapper;

    @InjectMocks
    private WarehouseLocationServiceImpl locationService;

    private User actor;
    private Warehouse warehouse;
    private WarehouseLocation zone;
    private WarehouseLocation bin;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.CEO);

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setCode("WH-HP");

        zone = new WarehouseLocation();
        zone.setId(20L);
        zone.setWarehouse(warehouse);
        zone.setCode("WH-HP.A");
        zone.setType(LocationType.ZONE);
        zone.setIsActive(true);

        bin = new WarehouseLocation();
        bin.setId(30L);
        bin.setWarehouse(warehouse);
        bin.setCode("WH-HP.A.01");
        bin.setType(LocationType.BIN);
        bin.setParent(zone);
        bin.setCapacityM3(BigDecimal.valueOf(10.0));
        bin.setCapacityKg(BigDecimal.valueOf(100.0));
        bin.setCurrentVolumeM3(BigDecimal.valueOf(2.0));
        bin.setCurrentWeightKg(BigDecimal.valueOf(20.0));
        bin.setIsActive(true);
    }

    @Test
    void createLocation_Zone_AutoPrefix() {
        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("A"); // suffix only
        req.setType("ZONE");

        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(locationRepository.existsByCode("WH-HP.A")).thenReturn(false);
        when(locationRepository.save(any(WarehouseLocation.class))).thenReturn(zone);

        locationService.createLocation(req, 1L);

        verify(locationRepository).save(argThat(loc -> loc.getCode().equals("WH-HP.A") && loc.getType() == LocationType.ZONE));
    }

    @Test
    void createLocation_Bin_RequiresParentZone() {
        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("01");
        req.setType("BIN");
        req.setParentId(null); // invalid for Bin

        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        assertThrows(IllegalArgumentException.class, () -> locationService.createLocation(req, 1L));
    }

    @Test
    void updateLocation_CapacityShrink_Failure() {
        WarehouseLocationRequest req = new WarehouseLocationRequest();
        req.setWarehouseId(10L);
        req.setCode("WH-HP.A.01");
        req.setType("BIN");
        req.setParentId(20L);
        req.setCapacityM3(BigDecimal.valueOf(1.0)); // Less than current used volume (2.0)
        req.setCapacityKg(BigDecimal.valueOf(100.0));

        when(locationRepository.findById(30L)).thenReturn(Optional.of(bin));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(locationRepository.findById(20L)).thenReturn(Optional.of(zone));

        assertThrows(IllegalArgumentException.class, () -> locationService.updateLocation(30L, req, 1L));
    }

    @Test
    void getCapacity_Bin() {
        when(locationRepository.findById(30L)).thenReturn(Optional.of(bin));

        CapacityResponse cap = locationService.getCapacity(30L);

        assertNotNull(cap);
        assertEquals(BigDecimal.valueOf(10.0), cap.getCapacityM3());
        assertEquals(BigDecimal.valueOf(2.0), cap.getUsedVolumeM3());
        assertEquals(BigDecimal.valueOf(8.0), cap.getAvailableVolumeM3());
    }
}
