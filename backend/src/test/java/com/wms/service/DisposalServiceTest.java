package com.wms.service;

import com.wms.dto.request.DisposalApproveRequest;
import com.wms.dto.request.DisposalCreateRequest;
import com.wms.dto.response.DisposalApproveResponse;
import com.wms.dto.response.DisposalCreateResponse;
import com.wms.entity.*;
import com.wms.enums.AdjustmentType;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisposalServiceTest {

    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private DamageReportRepository damageReportRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private AuditLogService auditLogService;

    private DisposalService disposalService;

    private User manager;
    private User ceo;
    private User storekeeper;
    private Warehouse warehouse;
    private Product product;
    private Batch batch;
    private WarehouseLocation quarantineLocation;
    private WarehouseLocation regularLocation;
    private Inventory quarantineInventory;

    @BeforeEach
    void setUp() {
        disposalService = new DisposalService(
                adjustmentRepository,
                damageReportRepository,
                inventoryRepository,
                warehouseRepository,
                productRepository,
                batchRepository,
                warehouseLocationRepository,
                userWarehouseAssignmentRepository,
                systemConfigRepository,
                auditLogService
        );

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("WH-HP");

        manager = new User();
        manager.setId(10L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        ceo = new User();
        ceo.setId(11L);
        ceo.setRole(UserRole.CEO);

        storekeeper = new User();
        storekeeper.setId(12L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        product = new Product();
        product.setId(100L);

        batch = new Batch();
        batch.setId(200L);

        quarantineLocation = new WarehouseLocation();
        quarantineLocation.setId(50L);
        quarantineLocation.setIsQuarantine(true);

        regularLocation = new WarehouseLocation();
        regularLocation.setId(51L);
        regularLocation.setIsQuarantine(false);

        quarantineInventory = new Inventory();
        quarantineInventory.setId(300L);
        quarantineInventory.setWarehouse(warehouse);
        quarantineInventory.setProduct(product);
        quarantineInventory.setBatch(batch);
        quarantineInventory.setLocation(quarantineLocation);
        quarantineInventory.setTotalQty(new BigDecimal("50.0"));
        quarantineInventory.setReservedQty(BigDecimal.ZERO);
        quarantineInventory.setCostPrice(new BigDecimal("1000000.00")); // 1M VND per unit
    }

    @Test
    void createProposal_happyPath_createsProposalSuccessfully() {
        DisposalCreateRequest request = new DisposalCreateRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setBatchId(200L);
        request.setLocationId(50L);
        request.setQuantity(new BigDecimal("10.0"));
        request.setCause("Damaged during shipping");

        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(10L)).thenReturn(List.of(1L));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(batchRepository.findById(200L)).thenReturn(Optional.of(batch));
        when(warehouseLocationRepository.findById(50L)).thenReturn(Optional.of(quarantineLocation));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));

        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(invocation -> {
            DamageReport dr = invocation.getArgument(0);
            dr.setId(888L);
            return dr;
        });
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> {
            Adjustment adj = invocation.getArgument(0);
            adj.setId(999L);
            return adj;
        });

        DisposalCreateResponse response = disposalService.createProposal(request, manager);

        assertNotNull(response);
        assertFalse(response.isConfirmed());
        assertEquals(0, response.getTotalValueEstimate().compareTo(new BigDecimal("10000000.00"))); // 10 * 1M = 10M VND

        verify(damageReportRepository, times(1)).save(any(DamageReport.class));
        verify(adjustmentRepository, times(1)).save(any(Adjustment.class));
        verify(auditLogService, times(1)).log(
                eq(manager), eq(AuditAction.QUARANTINE_DISPOSAL_CREATE), eq("ADJUSTMENT"),
                any(), anyString(), eq(1L), any(), any()
        );
    }

    @Test
    void createProposal_nonQuarantineLocation_throwsBusinessRuleViolation() {
        DisposalCreateRequest request = new DisposalCreateRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setBatchId(200L);
        request.setLocationId(51L); // regular location
        request.setQuantity(new BigDecimal("10.0"));

        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(10L)).thenReturn(List.of(1L));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(batchRepository.findById(200L)).thenReturn(Optional.of(batch));
        when(warehouseLocationRepository.findById(51L)).thenReturn(Optional.of(regularLocation));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () -> {
            disposalService.createProposal(request, manager);
        });

        assertTrue(ex.getMessage().contains("must be a quarantine location"));
    }

    @Test
    void createProposal_insufficientStock_throwsBusinessRuleViolation() {
        DisposalCreateRequest request = new DisposalCreateRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setBatchId(200L);
        request.setLocationId(50L);
        request.setQuantity(new BigDecimal("60.0")); // quarantine stock is only 50

        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(10L)).thenReturn(List.of(1L));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(batchRepository.findById(200L)).thenReturn(Optional.of(batch));
        when(warehouseLocationRepository.findById(50L)).thenReturn(Optional.of(quarantineLocation));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () -> {
            disposalService.createProposal(request, manager);
        });

        assertTrue(ex.getMessage().contains("exceeds available quarantine stock"));
    }

    @Test
    void approveDisposal_withinLimitManagerApprove_happyPath() {
        Adjustment adjustment = new Adjustment();
        adjustment.setId(500L);
        adjustment.setWarehouse(warehouse);
        adjustment.setProduct(product);
        adjustment.setBatch(batch);
        adjustment.setLocation(quarantineLocation);
        adjustment.setQuantityAdjustment(new BigDecimal("-10.0")); // absolute value = 10
        adjustment.setType(AdjustmentType.DISPOSAL);
        adjustment.setAdjustmentNumber("ADJ-TEST");

        SystemConfig config = new SystemConfig();
        config.setConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER");
        config.setConfigValue("100000000.00"); // 100M VND

        when(adjustmentRepository.findById(500L)).thenReturn(Optional.of(adjustment));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));
        when(systemConfigRepository.findByConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER"))
                .thenReturn(Optional.of(config));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(10L)).thenReturn(List.of(1L));

        DisposalApproveResponse response = disposalService.approveDisposal(500L, new DisposalApproveRequest(), manager);

        assertNotNull(response);
        assertTrue(response.isConfirmed());
        assertEquals(new BigDecimal("10.0"), response.getDeductedQty());
        assertEquals(new BigDecimal("40.0"), quarantineInventory.getTotalQty()); // 50 - 10 = 40

        verify(inventoryRepository, times(1)).save(quarantineInventory);
        verify(adjustmentRepository, times(1)).save(adjustment);
        verify(auditLogService, times(2)).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approveDisposal_exceedLimitManagerApprove_throwsForbidden() {
        Adjustment adjustment = new Adjustment();
        adjustment.setId(500L);
        adjustment.setWarehouse(warehouse);
        adjustment.setProduct(product);
        adjustment.setBatch(batch);
        adjustment.setLocation(quarantineLocation);
        adjustment.setQuantityAdjustment(new BigDecimal("-150.0")); // absolute value = 150
        adjustment.setType(AdjustmentType.DISPOSAL);

        quarantineInventory.setTotalQty(new BigDecimal("200.0")); // make sure enough stock

        SystemConfig config = new SystemConfig();
        config.setConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER");
        config.setConfigValue("100000000.00"); // 100M VND limit

        // 150 * 1M VND = 150M VND > 100M VND limit

        when(adjustmentRepository.findById(500L)).thenReturn(Optional.of(adjustment));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));
        when(systemConfigRepository.findByConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER"))
                .thenReturn(Optional.of(config));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(10L)).thenReturn(List.of(1L));

        ForbiddenReceiptWarehouseException ex = assertThrows(ForbiddenReceiptWarehouseException.class, () -> {
            disposalService.approveDisposal(500L, new DisposalApproveRequest(), manager);
        });

        assertTrue(ex.getMessage().contains("exceeds Trưởng kho limit"));
    }

    @Test
    void approveDisposal_exceedLimitCeoApprove_happyPath() {
        Adjustment adjustment = new Adjustment();
        adjustment.setId(500L);
        adjustment.setWarehouse(warehouse);
        adjustment.setProduct(product);
        adjustment.setBatch(batch);
        adjustment.setLocation(quarantineLocation);
        adjustment.setQuantityAdjustment(new BigDecimal("-150.0")); // 150 * 1M VND = 150M VND
        adjustment.setType(AdjustmentType.DISPOSAL);
        adjustment.setAdjustmentNumber("ADJ-TEST");

        quarantineInventory.setTotalQty(new BigDecimal("200.0"));

        SystemConfig config = new SystemConfig();
        config.setConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER");
        config.setConfigValue("100000000.00");

        when(adjustmentRepository.findById(500L)).thenReturn(Optional.of(adjustment));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));
        when(systemConfigRepository.findByConfigKey("DISPOSAL_LIMIT_WAREHOUSE_MANAGER"))
                .thenReturn(Optional.of(config));

        DisposalApproveResponse response = disposalService.approveDisposal(500L, new DisposalApproveRequest(), ceo);

        assertNotNull(response);
        assertTrue(response.isConfirmed());
        assertEquals(new BigDecimal("150.0"), response.getDeductedQty());
        assertEquals(new BigDecimal("50.0"), quarantineInventory.getTotalQty()); // 200 - 150 = 50

        verify(inventoryRepository, times(1)).save(quarantineInventory);
        verify(adjustmentRepository, times(1)).save(adjustment);
    }
}
