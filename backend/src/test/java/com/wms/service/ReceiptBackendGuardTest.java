package com.wms.service;

import com.wms.dto.request.ReceiptDecisionRequest;
import com.wms.dto.request.ReceiptRtvConfirmRequest;
import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.entity.Receipt;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.UserRole;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ForbiddenReceiptWarehouseException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.BatchRepository;
import com.wms.repository.DebitNoteRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseLocationRepository;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptBackendGuardTest {

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
    private ReceiptApprovalService approvalService;
    private QuarantineRtvService rtvService;

    private Receipt qcCompletedReceipt;
    private Receipt qcFailedReceipt;
    private User storekeeper;
    private User manager;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);

        storekeeper = new User();
        storekeeper.setId(6L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        manager = new User();
        manager.setId(5L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        qcCompletedReceipt = receipt(1L, "RCV-APPROVAL", ReceiptStatus.QC_COMPLETED, warehouse, 3);
        qcFailedReceipt = receipt(2L, "RCV-QC-FAILED", ReceiptStatus.QC_FAILED, warehouse, 4);

        receiptValidationService = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
        approvalService = new ReceiptApprovalService(
                receiptRepository,
                receiptItemRepository,
                batchRepository,
                inventoryRepository,
                warehouseLocationRepository,
                receiptValidationService,
                auditLogService
        );
        rtvService = new QuarantineRtvService(
                receiptRepository,
                receiptItemRepository,
                adjustmentRepository,
                debitNoteRepository,
                inventoryRepository,
                receiptValidationService,
                auditLogService
        );
    }

    @Test
    void approve_withStorekeeperRole_throwsForbidden() {
        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(3);

        assertThrows(ForbiddenReceiptWarehouseException.class,
                () -> approvalService.approveReceipt(1L, request, storekeeper));

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createRtv_staleReceiptVersion_throwsVersionConflictBeforeCreatingDocuments() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(99);
        request.setReason("Hàng lỗi");

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> rtvService.createRtv(2L, request, manager));

        assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
        verify(adjustmentRepository, never()).save(any());
        verify(debitNoteRepository, never()).save(any());
    }

    @Test
    void confirmRtv_staleReceiptVersion_throwsVersionConflictBeforeInventoryDeduction() {
        ReceiptRtvConfirmRequest request = new ReceiptRtvConfirmRequest();
        request.setExpectedVersion(99);
        request.setReturnedQty(java.math.BigDecimal.TEN);

        when(receiptRepository.findById(2L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(6L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> rtvService.confirmRtv(2L, request, storekeeper));

        assertTrue(ex.getMessage().contains("INVENTORY_VERSION_CONFLICT"));
        verify(inventoryRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
    }

    private Receipt receipt(Long id, String number, ReceiptStatus status, Warehouse warehouse, Integer version) {
        Receipt receipt = new Receipt();
        receipt.setId(id);
        receipt.setReceiptNumber(number);
        receipt.setStatus(status);
        receipt.setWarehouse(warehouse);
        receipt.setDocumentDate(LocalDate.now());
        receipt.setVersion(version);
        receipt.setCreatedAt(OffsetDateTime.now());
        receipt.setUpdatedAt(OffsetDateTime.now());
        return receipt;
    }
}
