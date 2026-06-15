package com.wms.service;

import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.response.RtvActionResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.*;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptService.confirmRtv() — US-WMS-04.
 *
 * <p>Covers: RTV confirm full quantity happy path, partial quantity mismatch (HTTP 422),
 * duplicate confirmation (HTTP 409), quarantine inventory non-negative invariant.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReceiptRtvConfirmServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private DebitNoteRepository debitNoteRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private AuditLogService auditLogService;

    private ReceiptValidationService receiptValidationService;
    private QuarantineRtvService receiptService;

    private User storekeeper;
    private Warehouse warehouse;
    private Receipt qcFailedReceipt;
    private ReceiptItem failedItem;
    private Batch quarantineBatch;
    private WarehouseLocation quarantineLocation;
    private Adjustment pendingRtv;
    private Inventory quarantineInventory;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(10L);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        Product product = new Product();
        product.setId(100L);

        quarantineBatch = new Batch();
        quarantineBatch.setId(200L);

        quarantineLocation = new WarehouseLocation();
        quarantineLocation.setId(50L);
        quarantineLocation.setIsQuarantine(true);

        qcFailedReceipt = new Receipt();
        qcFailedReceipt.setId(1L);
        qcFailedReceipt.setReceiptNumber("RCV-QC-FAIL-001");
        qcFailedReceipt.setStatus(ReceiptStatus.QC_FAILED);
        qcFailedReceipt.setWarehouse(warehouse);
        qcFailedReceipt.setDocumentDate(LocalDate.now());
        qcFailedReceipt.setVersion(3);
        qcFailedReceipt.setCreatedAt(OffsetDateTime.now());
        qcFailedReceipt.setUpdatedAt(OffsetDateTime.now());

        failedItem = new ReceiptItem();
        failedItem.setId(10L);
        failedItem.setProduct(product);
        failedItem.setActualQty(20);
        failedItem.setUnitCost(BigDecimal.valueOf(50));
        failedItem.setBatch(quarantineBatch);
        failedItem.setLocation(quarantineLocation);
        failedItem.setQcResult(QcResult.FAILED);

        pendingRtv = new Adjustment();
        pendingRtv.setId(100L);
        pendingRtv.setAdjustmentNumber("ADJ-20260611-ABCDEF");
        pendingRtv.setType(AdjustmentType.RETURN_TO_VENDOR);
        pendingRtv.setReferenceType("RECEIPT");
        pendingRtv.setReferenceId(1L);
        pendingRtv.setQuantityAdjustment(BigDecimal.valueOf(-20));
        pendingRtv.setWarehouse(warehouse);
        pendingRtv.setProduct(product);
        // approvedAt == null means PENDING

        quarantineInventory = new Inventory();
        quarantineInventory.setId(300L);
        quarantineInventory.setWarehouse(warehouse);
        quarantineInventory.setProduct(product);
        quarantineInventory.setBatch(quarantineBatch);
        quarantineInventory.setLocation(quarantineLocation);
        quarantineInventory.setTotalQty(BigDecimal.valueOf(20));
        quarantineInventory.setReservedQty(BigDecimal.ZERO);
        quarantineInventory.setUpdatedAt(OffsetDateTime.now());

        receiptValidationService = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
        receiptService = new QuarantineRtvService(
                receiptRepository,
                receiptItemRepository,
                adjustmentRepository,
                debitNoteRepository,
                inventoryRepository,
                receiptValidationService,
                auditLogService
        );
    }

    // -----------------------------------------------------------------------
    // Happy path: confirm full quantity
    // -----------------------------------------------------------------------

    @Test
    void confirmRtv_fullQuantity_deductsQuarantineInventory() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(20)); // Exact match

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference("RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR))
                .thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference("RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR))
                .thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(10L, 100L, 200L, 50L))
                .thenReturn(Optional.of(quarantineInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        RtvActionResponse response = receiptService.confirmRtv(1L, request, storekeeper);

        assertTrue(response.isConfirmed());
        assertNotNull(response.getConfirmedAt());
        assertEquals(BigDecimal.valueOf(20), response.getQuarantineQty());

        // Quarantine inventory must be reduced to 0
        verify(inventoryRepository).save(argThat(inv ->
                inv.getTotalQty().compareTo(BigDecimal.ZERO) == 0
        ));

        // Receipt status remains QC_FAILED
        verify(receiptRepository).save(argThat(r -> r.getStatus() == ReceiptStatus.QC_FAILED));

        // RTV adjustment marked confirmed
        verify(adjustmentRepository).save(argThat(adj ->
                adj.getApprovedBy() != null && adj.getApprovedAt() != null
        ));
    }

    @Test
    void confirmRtv_happyPath_auditLogContainsDeductedQty() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(20));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference(any(), any(), any())).thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference(any(), any(), any())).thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(quarantineInventory));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        receiptService.confirmRtv(1L, request, storekeeper);

        // QUARANTINE_RTV_CONFIRM audit log should be created
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.QUARANTINE_RTV_CONFIRM),
                eq("ADJUSTMENT"), any(), any(), eq(10L), any(), any());
    }

    // -----------------------------------------------------------------------
    // Partial quantity mismatch (HTTP 422)
    // -----------------------------------------------------------------------

    @Test
    void confirmRtv_partialQuantity_throwsBusinessRuleViolation() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(18)); // Only 18 of 20 units

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference(any(), any(), any())).thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference(any(), any(), any())).thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmRtv(1L, request, storekeeper));

        assertTrue(ex.getMessage().contains("RTV_QUANTITY_MISMATCH"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void confirmRtv_moreQuantityThanQuarantine_throwsBusinessRuleViolation() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(25)); // More than 20

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference(any(), any(), any())).thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference(any(), any(), any())).thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));

        assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmRtv(1L, request, storekeeper));

        verify(inventoryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Duplicate confirmation (HTTP 409)
    // -----------------------------------------------------------------------

    @Test
    void confirmRtv_alreadyConfirmed_throwsBusinessRuleViolation() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(20));

        Adjustment confirmedRtv = new Adjustment();
        confirmedRtv.setId(100L);
        confirmedRtv.setApprovedAt(OffsetDateTime.now()); // Already confirmed

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference("RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR))
                .thenReturn(Optional.of(confirmedRtv));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmRtv(1L, request, storekeeper));

        assertTrue(ex.getMessage().contains("RTV_ALREADY_CONFIRMED"));
        verify(inventoryRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Quarantine inventory non-negative invariant
    // -----------------------------------------------------------------------

    @Test
    void confirmRtv_deductionWouldResultInNegativeInventory_throwsBusinessRuleViolation() {
        // Inventory has only 10 units but RTV says 20 were quarantined
        quarantineInventory.setTotalQty(BigDecimal.valueOf(10)); // Inconsistent state

        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(20));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference(any(), any(), any())).thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference(any(), any(), any())).thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(quarantineInventory));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmRtv(1L, request, storekeeper));

        assertTrue(ex.getMessage().contains("INVENTORY_INVARIANT_VIOLATED"));
        // Inventory not saved when negative would result
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void confirmRtv_receiptStatusRemainsQcFailed() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(3);
        request.setReturnedQty(BigDecimal.valueOf(20));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(adjustmentRepository.findConfirmedRtvByReference(any(), any(), any())).thenReturn(Optional.empty());
        when(adjustmentRepository.findPendingRtvByReference(any(), any(), any())).thenReturn(Optional.of(pendingRtv));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(quarantineInventory));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        receiptService.confirmRtv(1L, request, storekeeper);

        // Receipt status must remain QC_FAILED after RTV confirmation
        verify(receiptRepository).save(argThat(r -> r.getStatus() == ReceiptStatus.QC_FAILED));
    }
}
