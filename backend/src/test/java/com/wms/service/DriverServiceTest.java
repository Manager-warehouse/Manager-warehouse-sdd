package com.wms.service;

import com.wms.dto.request.DriverRequest;
import com.wms.entity.Driver;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.DriverStatus;
import com.wms.enums.UserRole;
import com.wms.repository.DriverRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.DriverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private com.wms.mapper.MasterDataMapper mapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DriverServiceImpl driverService;

    private User actor;
    private User driverUser;
    private Warehouse warehouse;
    private Driver driver;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);

        driverUser = new User();
        driverUser.setId(3L);
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setPhone("0987654321");

        warehouse = new Warehouse();
        warehouse.setId(2L);

        driver = new Driver();
        driver.setId(4L);
        driver.setWarehouse(warehouse);
        driver.setUser(driverUser);
        driver.setLicenseNumber("LX-99999");
        driver.setStatus(DriverStatus.AVAILABLE);
        driver.setIsActive(true);
    }

    @Test
    void createDriver_Success_PhoneFallback() {
        DriverRequest req = new DriverRequest();
        req.setWarehouseId(2L);
        req.setUserId(3L);
        req.setFullName("Nguyen Van A");
        req.setLicenseNumber("LX-99999");
        req.setLicenseExpiry(LocalDate.now().plusYears(5));
        req.setPhone(""); // empty to test fallback

        when(driverRepository.existsByLicenseNumber("LX-99999")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(3L)).thenReturn(Optional.of(driverUser));
        when(driverRepository.existsByUserId(3L)).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        driverService.createDriver(req, 1L);

        verify(driverRepository).save(argThat(d -> d.getPhone().equals("0987654321"))); // Falls back to driverUser.phone
        verify(auditLogService).log(eq(actor), eq(AuditAction.CREATE), eq("Driver"), any(), eq("LX-99999"), any(), any(), any());
    }

    @Test
    void createDriver_InvalidRole_ThrowsException() {
        DriverRequest req = new DriverRequest();
        req.setWarehouseId(2L);
        req.setUserId(3L);
        driverUser.setRole(UserRole.STOREKEEPER); // Not DRIVER

        when(driverRepository.existsByLicenseNumber(any())).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(3L)).thenReturn(Optional.of(driverUser));

        assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(req, 1L));
    }

    @Test
    void deactivateDriver_OnTrip_ThrowsException() {
        driver.setStatus(DriverStatus.ON_TRIP);
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));

        assertThrows(IllegalArgumentException.class, () -> driverService.deactivateDriver(4L, 1L));
    }
}
