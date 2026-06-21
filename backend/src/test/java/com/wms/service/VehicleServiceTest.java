package com.wms.service;

import com.wms.dto.request.VehicleRequest;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.VehicleStatus;
import com.wms.repository.UserRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.VehicleServiceImpl;
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
        vehicle.setPlateNumber("29C-12345");
        vehicle.setWarehouse(warehouse);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setIsActive(true);
    }

    @Test
    void createVehicle_Success() {
        VehicleRequest req = new VehicleRequest();
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
        verify(auditLogService).log(eq(actor), eq(AuditAction.CREATE), eq("Vehicle"), any(), eq("29C-12345"), any(), any(), any());
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
        verify(auditLogService).log(eq(actor), eq(AuditAction.SOFT_DELETE), eq("Vehicle"), eq(5L), eq("29C-12345"), any(), any(), any());
    }
}
