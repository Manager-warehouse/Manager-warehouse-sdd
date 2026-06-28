package com.wms.service;

import com.wms.dto.request.ReceiptPutawayItem;
import com.wms.dto.request.ReceiptPutawayRequest;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.Batch;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.BatchRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptPutawayServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock
    private AuditLogService auditLogService;

    private ReceiptValidationService receiptValidationService;
    private ReceiptApprovalService receiptService;

    private User storekeeper;
    private Warehouse warehouse;
    private Receipt approvedReceipt;
    private Product product;
    private Batch batch;
    private ReceiptItem item;
    private WarehouseLocation regularBin;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(10L);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        product = new Product();
        product.setId(100L);
        product.setVolumeM3(BigDecimal.valueOf(0.50));
        product.setWeightKg(BigDecimal.valueOf(2.00));

        batch = new Batch();
        batch.setId(200L);

        approvedReceipt = new Receipt();
        approvedReceipt.setId(1L);
        approvedReceipt.setReceiptNumber("RCV-PUTAWAY-001");
        approvedReceipt.setStatus(ReceiptStatus.APPROVED);
        approvedReceipt.setWarehouse(warehouse);
        approvedReceipt.setDocumentDate(LocalDate.now());
        approvedReceipt.setVersion(5);
        approvedReceipt.setCreatedAt(OffsetDateTime.now());
        approvedReceipt.setUpdatedAt(OffsetDateTime.now());

        item = new ReceiptItem();
        item.setId(11L);
        item.setProduct(product);
        item.setBatch(batch);
        item.setActualQty(10);
        item.setUnitCost(BigDecimal.valueOf(30));

        regularBin = new WarehouseLocation();
        regularBin.setId(50L);
        regularBin.setWarehouse(warehouse);
        regularBin.setIsQuarantine(false);
        regularBin.setCapacityM3(BigDecimal.valueOf(10));
        regularBin.setCapacityKg(BigDecimal.valueOf(30));
        regularBin.setCurrentVolumeM3(BigDecimal.ONE);
        regularBin.setCurrentWeightKg(BigDecimal.valueOf(5));

        receiptValidationService = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
        receiptService = new ReceiptApprovalService(
                receiptRepository,
                receiptItemRepository,
                batchRepository,
                inventoryRepository,
                warehouseLocationRepository,
                receiptValidationService,
                auditLogService);
    }

    @Test
    void completePutaway_happyPath_increasesInventoryAndBinOccupancy() {
        ReceiptPutawayRequest request = request();
        Inventory inventory = existingInventory(BigDecimal.ZERO);

        commonStubs();
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(10L, 100L, 200L, 50L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptItemRepository.save(any(ReceiptItem.class))).thenReturn(item);
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        ReceiptActionResponse response = receiptService.completePutaway(1L, request, storekeeper);

        assertEquals(ReceiptStatus.APPROVED, response.getStatus());
        verify(inventoryRepository).save(argThat(inv -> inv.getTotalQty().compareTo(BigDecimal.TEN) == 0));
        verify(warehouseLocationRepository)
                .save(argThat(location -> location.getCurrentVolumeM3().compareTo(BigDecimal.valueOf(6.00)) == 0
                        && location.getCurrentWeightKg().compareTo(BigDecimal.valueOf(25.00)) == 0));
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.INVENTORY_UPDATE),
                eq("INVENTORY"), eq(300L), any(), eq(10L), any(), any());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.RECEIPT_PUTAWAY_COMPLETE),
                eq("RECEIPT"), eq(1L), eq("RCV-PUTAWAY-001"), eq(10L), any(), any());
    }

    @Test
    void completePutaway_quarantineLocation_throwsInvalidLocation() {
        regularBin.setIsQuarantine(true);
        ReceiptPutawayRequest request = request();

        commonStubs();

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.completePutaway(1L, request, storekeeper));

        assertEquals(true, ex.getMessage().contains("INVALID_LOCATION"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void completePutaway_capacityExceeded_throwsBeforeInventoryMutation() {
        regularBin.setCapacityM3(BigDecimal.valueOf(5));
        ReceiptPutawayRequest request = request();

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(approvedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(warehouseLocationRepository.findById(50L)).thenReturn(Optional.of(regularBin));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(item));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.completePutaway(1L, request, storekeeper));

        assertEquals(true, ex.getMessage().contains("BIN_CAPACITY_EXCEEDED"));
        verify(inventoryRepository, never()).save(any());
        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    void completePutaway_missingBatch_throwsBeforeInventoryMutation() {
        item.setBatch(null);
        ReceiptPutawayRequest request = request();

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(approvedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(item));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.completePutaway(1L, request, storekeeper));

        assertEquals(true, ex.getMessage().contains("MISSING_BATCH"));
        verify(inventoryRepository, never()).save(any());
    }

    private void commonStubs() {
        when(receiptRepository.findById(1L)).thenReturn(Optional.of(approvedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(warehouseLocationRepository.findById(50L)).thenReturn(Optional.of(regularBin));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(item));
    }

    private ReceiptPutawayRequest request() {
        ReceiptPutawayRequest request = new ReceiptPutawayRequest();
        request.setExpectedVersion(5);

        ReceiptPutawayItem putawayItem = new ReceiptPutawayItem();
        putawayItem.setReceiptItemId(11L);
        putawayItem.setLocationId(50L);

        request.setItems(List.of(putawayItem));
        return request;
    }

    private Inventory existingInventory(BigDecimal totalQty) {
        Inventory inventory = new Inventory();
        inventory.setId(300L);
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setLocation(regularBin);
        inventory.setTotalQty(totalQty);
        inventory.setReservedQty(BigDecimal.ZERO);
        inventory.setUpdatedAt(OffsetDateTime.now());
        return inventory;
    }
}
