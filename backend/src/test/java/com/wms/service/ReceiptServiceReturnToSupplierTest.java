package com.wms.service;

import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptReturnConfirmRequest;
import com.wms.dto.response.ReceiptActionResponse;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptService reject and confirmReturnToSupplier — US-WMS-05.
 *
 * <p>Covers: reject happy path, missing reason, invalid status, stale version,
 * return-confirm happy path, wrong state for confirm.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReceiptServiceReturnToSupplierTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private DebitNoteRepository debitNoteRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ReceiptApprovalService receiptService;

    private User manager;
    private User storekeeper;
    private Warehouse warehouse;
    private Receipt qcCompletedReceipt;
    private Receipt returnPendingReceipt;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setCode("WH-HN");

        manager = new User();
        manager.setId(5L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        qcCompletedReceipt = new Receipt();
        qcCompletedReceipt.setId(1L);
        qcCompletedReceipt.setReceiptNumber("RCV-2026-002");
        qcCompletedReceipt.setStatus(ReceiptStatus.QC_COMPLETED);
        qcCompletedReceipt.setWarehouse(warehouse);
        qcCompletedReceipt.setDocumentDate(LocalDate.now());
        qcCompletedReceipt.setVersion(2);
        qcCompletedReceipt.setCreatedAt(OffsetDateTime.now());
        qcCompletedReceipt.setUpdatedAt(OffsetDateTime.now());

        returnPendingReceipt = new Receipt();
        returnPendingReceipt.setId(2L);
        returnPendingReceipt.setReceiptNumber("RCV-2026-003");
        returnPendingReceipt.setStatus(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING);
        returnPendingReceipt.setWarehouse(warehouse);
        returnPendingReceipt.setDocumentDate(LocalDate.now());
        returnPendingReceipt.setVersion(4);
        returnPendingReceipt.setCreatedAt(OffsetDateTime.now());
        returnPendingReceipt.setUpdatedAt(OffsetDateTime.now());
    }

    // -----------------------------------------------------------------------
    // rejectReceipt — happy path
    // -----------------------------------------------------------------------

    @Test
    void rejectReceipt_happyPath_statusBecomesReturnPending() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        request.setReason("Hàng bị ẩm, không đạt tiêu chuẩn chất lượng");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        ReceiptActionResponse response = receiptService.rejectReceipt(1L, request, manager);

        assertNotNull(response);
        assertEquals(ReceiptStatus.RETURN_TO_SUPPLIER_PENDING, response.getStatus());

        verify(receiptRepository).save(argThat(r ->
                r.getStatus() == ReceiptStatus.RETURN_TO_SUPPLIER_PENDING
                && "Hàng bị ẩm, không đạt tiêu chuẩn chất lượng".equals(r.getRejectionReason())));
        verify(auditLogService).log(eq(manager), eq(AuditAction.RECEIPT_REJECT),
                eq("RECEIPT"), eq(1L), eq("RCV-2026-002"), eq(10L), any(), any());
    }

    @Test
    void rejectReceipt_happyPath_doesNotCreateBatchOrInventoryOrRtv() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        request.setReason("Hàng lỗi mẫu");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        receiptService.rejectReceipt(1L, request, manager);

        // Rejection must NOT create batch, inventory, RTV, or Debit Note
        verify(batchRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
        verify(debitNoteRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // rejectReceipt — missing reason
    // -----------------------------------------------------------------------

    @Test
    void rejectReceipt_missingReason_throwsIllegalArgument() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        // reason intentionally omitted - check happens before assertWarehouseAssignment

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> receiptService.rejectReceipt(1L, request, manager));

        assertTrue(ex.getMessage().contains("REASON_REQUIRED"));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void rejectReceipt_blankReason_throwsIllegalArgument() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        request.setReason("   ");
        // blank reason check happens before assertWarehouseAssignment

        assertThrows(IllegalArgumentException.class,
                () -> receiptService.rejectReceipt(1L, request, manager));
    }

    // -----------------------------------------------------------------------
    // rejectReceipt — invalid status
    // -----------------------------------------------------------------------

    @Test
    void rejectReceipt_alreadyApproved_throwsReceiptAlreadyDecided() {
        qcCompletedReceipt.setStatus(ReceiptStatus.APPROVED);
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        request.setReason("reason");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        assertThrows(ReceiptAlreadyDecidedException.class,
                () -> receiptService.rejectReceipt(1L, request, manager));
    }

    @Test
    void rejectReceipt_qcFailed_throwsBusinessRuleViolation() {
        qcCompletedReceipt.setStatus(ReceiptStatus.QC_FAILED);
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(2);
        request.setReason("reason");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcCompletedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.rejectReceipt(1L, request, manager));
    }

    // -----------------------------------------------------------------------
    // confirmReturnToSupplier — happy path
    // -----------------------------------------------------------------------

    @Test
    void confirmReturnToSupplier_happyPath_statusBecomesReturnedToSupplier() {
        ReceiptReturnConfirmRequest request = new ReceiptReturnConfirmRequest();
        request.setExpectedVersion(4);
        request.setHandoverNote("Tài xế Nguyễn Văn A, xe 51A-12345");

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(returnPendingReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(i -> i.getArgument(0));

        ReceiptActionResponse response = receiptService.confirmReturnToSupplier(2L, request, storekeeper);

        assertNotNull(response);
        assertEquals(ReceiptStatus.RETURNED_TO_SUPPLIER, response.getStatus());

        verify(receiptRepository).save(argThat(r -> r.getStatus() == ReceiptStatus.RETURNED_TO_SUPPLIER));
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.RECEIPT_RETURN_CONFIRM),
                eq("RECEIPT"), eq(2L), eq("RCV-2026-003"), eq(10L), any(), any());
    }

    @Test
    void confirmReturnToSupplier_happyPath_doesNotCreateInventoryOrRtv() {
        ReceiptReturnConfirmRequest request = new ReceiptReturnConfirmRequest();
        request.setExpectedVersion(4);

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(returnPendingReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));
        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        receiptService.confirmReturnToSupplier(2L, request, storekeeper);

        verify(inventoryRepository, never()).save(any());
        verify(batchRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
        verify(debitNoteRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // confirmReturnToSupplier — wrong state
    // -----------------------------------------------------------------------

    @Test
    void confirmReturnToSupplier_wrongState_approved_throwsBusinessRuleViolation() {
        returnPendingReceipt.setStatus(ReceiptStatus.APPROVED);
        ReceiptReturnConfirmRequest request = new ReceiptReturnConfirmRequest();
        request.setExpectedVersion(4);

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(returnPendingReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmReturnToSupplier(2L, request, storekeeper));

        assertTrue(ex.getMessage().contains("RETURN_TO_SUPPLIER_PENDING"));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void confirmReturnToSupplier_staleVersion_throwsBusinessRuleViolation() {
        ReceiptReturnConfirmRequest request = new ReceiptReturnConfirmRequest();
        request.setExpectedVersion(99); // Stale

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(returnPendingReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.confirmReturnToSupplier(2L, request, storekeeper));

        assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
    }
}
