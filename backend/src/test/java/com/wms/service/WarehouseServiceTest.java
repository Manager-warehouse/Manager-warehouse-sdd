package com.wms.service;

import com.wms.dto.request.WarehouseRequest;
import com.wms.dto.response.WarehouseResponse;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.mapper.MasterDataMapper;
import com.wms.repository.InventoryRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.service.impl.WarehouseServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private WarehouseLocationRepository locationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private MasterDataMapper mapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;


    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private User actor;
    private User manager;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setEmail("ceo@wms.com");
        actor.setFullName("CEO Admin");
        actor.setRole(UserRole.CEO);

        manager = new User();
        manager.setId(2L);
        manager.setEmail("manager@wms.com");
        manager.setFullName("Warehouse Manager");
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setCode("WH-HP");
        warehouse.setName("Hai Phong Warehouse");
        warehouse.setType(WarehouseType.PHYSICAL);
        warehouse.setIsActive(true);
        warehouse.setManager(manager);
    }

    @Test
    void createWarehouse_Success_Physical() {
        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-HP");
        req.setName("Hai Phong Warehouse");
        req.setType("PHYSICAL");
        req.setManagerId(2L);

        when(warehouseRepository.existsByCode("WH-HP")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);
        when(mapper.toResponse(any(Warehouse.class))).thenReturn(new WarehouseResponse());

        WarehouseResponse res = warehouseService.createWarehouse(req, 1L);

        assertNotNull(res);
        verify(warehouseRepository).save(any(Warehouse.class));
        verify(auditLogService).log(eq(actor), eq(AuditAction.CREATE), eq("Warehouse"), any(), eq("WH-HP"), any(), any(), any());
        verify(locationRepository, never()).save(any(WarehouseLocation.class)); // Not IN_TRANSIT
    }

    @Test
    void createWarehouse_Success_InTransit() {
        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-TRANSIT");
        req.setName("In-Transit Warehouse");
        req.setType("IN_TRANSIT");

        Warehouse transitWarehouse = new Warehouse();
        transitWarehouse.setId(11L);
        transitWarehouse.setCode("WH-TRANSIT");
        transitWarehouse.setType(WarehouseType.IN_TRANSIT);

        when(warehouseRepository.existsByCode("WH-TRANSIT")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(transitWarehouse);
        when(locationRepository.save(any(WarehouseLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        warehouseService.createWarehouse(req, 1L);

        verify(warehouseRepository).save(any(Warehouse.class));
        verify(locationRepository, times(2)).save(any(WarehouseLocation.class)); // 1 zone, 1 bin
        verify(auditLogService, times(3)).log(eq(actor), any(AuditAction.class), anyString(), any(), anyString(), any(), any(), any());
    }

    @Test
    void createWarehouse_DuplicateCode_ThrowsException() {
        WarehouseRequest req = new WarehouseRequest();
        req.setCode("WH-HP");

        when(warehouseRepository.existsByCode("WH-HP")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> warehouseService.createWarehouse(req, 1L));
    }

    @Test
    void deactivateWarehouse_Success_NoStock() {
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.existsByWarehouseIdAndTotalQtyGreaterThan(10L, BigDecimal.ZERO)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        warehouseService.deactivateWarehouse(10L, 1L);

        assertFalse(warehouse.getIsActive());
        verify(warehouseRepository).save(warehouse);
        verify(auditLogService).log(eq(actor), eq(AuditAction.SOFT_DELETE), eq("Warehouse"), eq(10L), eq("WH-HP"), any(), any(), any());
    }

    @Test
    void deactivateWarehouse_Failure_HasStock() {
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.existsByWarehouseIdAndTotalQtyGreaterThan(10L, BigDecimal.ZERO)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> warehouseService.deactivateWarehouse(10L, 1L));
        assertTrue(warehouse.getIsActive());
    }

    @Test
    void getAllWarehouses_Admin_ReturnsAll() {
        Warehouse w1 = new Warehouse();
        w1.setId(10L);
        w1.setIsActive(true);
        Warehouse w2 = new Warehouse();
        w2.setId(20L);
        w2.setIsActive(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(w1, w2));
        when(mapper.toResponse(any(Warehouse.class))).thenReturn(new WarehouseResponse());

        var result = warehouseService.getAllWarehouses(true, 1L);
        assertEquals(2, result.size());
    }

    @Test
    void getAllWarehouses_RestrictedUser_ReturnsOnlyAssigned() {
        Warehouse w1 = new Warehouse();
        w1.setId(10L);
        w1.setIsActive(true);
        Warehouse w2 = new Warehouse();
        w2.setId(20L);
        w2.setIsActive(true);

        User restrictedUser = new User();
        restrictedUser.setId(3L);
        restrictedUser.setRole(UserRole.STOREKEEPER);

        when(userRepository.findById(3L)).thenReturn(Optional.of(restrictedUser));
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(w1, w2));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(10L));
        when(mapper.toResponse(w1)).thenReturn(new WarehouseResponse());

        var result = warehouseService.getAllWarehouses(true, 3L);
        assertEquals(1, result.size());
    }
}

