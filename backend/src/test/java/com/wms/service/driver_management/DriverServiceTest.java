package com.wms.service.driver_management;


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

import com.wms.dto.request.driver_management.DriverRequest;
import com.wms.dto.response.driver_management.DriverResponse;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.repository.driver_management.DriverRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.driver_management.impl.DriverServiceImpl;
import com.wms.service.audit_trail.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
    private UserWarehouseAssignmentRepository assignmentRepository;
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
        actor.setRole(UserRole.ADMIN);

        driverUser = new User();
        driverUser.setId(3L);
        driverUser.setRole(UserRole.DRIVER);
        driverUser.setFullName("Driver Account Name");
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
        when(mapper.toResponse(any(Driver.class))).thenReturn(new DriverResponse());
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(1L));

        driverService.createDriver(req, 1L);

        verify(driverRepository).save(argThat(d ->
                d.getFullName().equals("Driver Account Name") && d.getPhone().equals("0987654321")));
        verify(auditLogService).log(eq(actor), eq(AuditAction.CREATE), eq("Driver"), any(), eq("LX-99999"), any(),
                any(), any());
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
    void createDriver_OutsideDispatcherWarehouse_ThrowsException() {
        actor.setRole(UserRole.DISPATCHER);
        DriverRequest req = new DriverRequest();
        req.setUserId(3L);
        req.setLicenseNumber("LX-99999");
        req.setWarehouseId(2L);

        when(driverRepository.existsByLicenseNumber("LX-99999")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(3L)).thenReturn(Optional.of(driverUser));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(new Warehouse()));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(2L));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> driverService.createDriver(req, 1L));

        assertEquals("WAREHOUSE_SCOPE_REQUIRED", error.getMessage());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void getDriverUserCandidates_ReturnsDriverUsersWithWarehouses() {
        actor.setRole(UserRole.DISPATCHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findByRole(UserRole.DRIVER)).thenReturn(List.of(driverUser));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(1L));

        var result = driverService.getDriverUserCandidates(1L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
        assertEquals(UserRole.DRIVER, result.get(0).getRole());
        assertEquals(List.of(1L), result.get(0).getWarehouses());
    }

    @Test
    void getDriverUserCandidates_FiltersUsersOutsideDispatcherWarehouse() {
        actor.setRole(UserRole.DISPATCHER);
        User hnDriver = new User();
        hnDriver.setId(5L);
        hnDriver.setRole(UserRole.DRIVER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findByRole(UserRole.DRIVER)).thenReturn(List.of(driverUser, hnDriver));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(1L, 2L));
        when(assignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(2L));

        var result = driverService.getDriverUserCandidates(1L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void deactivateDriver_OnTrip_ThrowsException() {
        driver.setStatus(DriverStatus.ON_TRIP);
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));

        assertThrows(IllegalArgumentException.class, () -> driverService.deactivateDriver(4L, 1L));
    }

    @Test
    void deactivateDriver_DispatcherWithinWarehouse_Success() {
        actor.setRole(UserRole.DISPATCHER);
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(2L));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(2L));
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        driverService.deactivateDriver(4L, 1L);

        assertFalse(driver.getIsActive());
        verify(auditLogService).log(eq(actor), eq(AuditAction.SOFT_DELETE), eq("Driver"), eq(4L), eq("LX-99999"),
                any(), any(), any());
    }

    @Test
    void deactivateDriver_NonDispatcher_ThrowsException() {
        actor.setRole(UserRole.ADMIN);
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> driverService.deactivateDriver(4L, 1L));

        assertEquals("DISPATCHER_ROLE_REQUIRED", ex.getMessage());
        assertTrue(driver.getIsActive());
        verify(driverRepository, never()).save(any());
    }

    @Test
    void reactivateDriver_DispatcherWithinWarehouse_Success() {
        actor.setRole(UserRole.DISPATCHER);
        driver.setIsActive(false);
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(2L));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(2L));
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);
        when(mapper.toResponse(driver)).thenReturn(new DriverResponse());

        driverService.reactivateDriver(4L, 1L);

        assertTrue(driver.getIsActive());
        verify(auditLogService).log(eq(actor), eq(AuditAction.UPDATE), eq("Driver"), eq(4L), eq("LX-99999"),
                any(), any(), any());
    }

    @Test
    void updateStatus_OnTrip_ThrowsException() {
        when(driverRepository.findById(4L)).thenReturn(Optional.of(driver));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> driverService.updateStatus(4L, "ON_TRIP", 1L));

        assertEquals("DRIVER_ON_TRIP_STATUS_SYSTEM_MANAGED", ex.getMessage());
        verify(driverRepository, never()).save(any());
    }
}
