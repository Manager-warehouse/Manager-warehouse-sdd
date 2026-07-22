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

import com.wms.dto.request.VehicleRequest;
import com.wms.entity.access_control.User;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.repository.UserRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.fleet_management.impl.VehicleServiceImpl;
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
public class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.wms.mapper.MasterDataMapper mapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private User actor;
    private Warehouse warehouse;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("HP-01");

        vehicle = new Vehicle();
        vehicle.setId(5L);
        vehicle.setWarehouse(warehouse);
        vehicle.setPlateNumber("29C-12345");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setIsActive(true);
    }

    @Test
    void createVehicle_Success() {
        VehicleRequest req = new VehicleRequest();
        req.setWarehouseId(2L);
        req.setPlateNumber("29C-12345");
        req.setVehicleType("Container 40ft");
        req.setMaxWeightKg(BigDecimal.valueOf(25000.0));
        req.setWarehouseId(warehouse.getId());

        when(vehicleRepository.existsByPlateNumber("29C-12345")).thenReturn(false);
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        vehicleService.createVehicle(req, 1L);

        verify(vehicleRepository).save(any(Vehicle.class));
        verify(auditLogService).log(eq(actor), eq(AuditAction.CREATE), eq("Vehicle"), any(), eq("29C-12345"), any(),
                any(), any());
    }

    @Test
    void deactivateVehicle_OnTrip_ThrowsException() {
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        assertThrows(IllegalArgumentException.class, () -> vehicleService.deactivateVehicle(5L, 1L));
        assertTrue(vehicle.getIsActive());
    }

    @Test
    void deactivateVehicle_Available_Success() {
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        vehicleService.deactivateVehicle(5L, 1L);

        assertFalse(vehicle.getIsActive());
        verify(auditLogService).log(eq(actor), eq(AuditAction.SOFT_DELETE), eq("Vehicle"), eq(5L), eq("29C-12345"),
                any(), any(), any());
    }
}
