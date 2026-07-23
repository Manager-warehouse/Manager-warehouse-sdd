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

import com.wms.dto.request.ReceiptRtvCreateRequest;
import com.wms.dto.response.RtvActionResponse;
import com.wms.exception.*;
import com.wms.repository.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReceiptService.createRtv() — US-WMS-04.
 *
 * <p>
 * Covers: RTV create happy path, duplicate RTV, non-QC_FAILED status,
 * missing quarantine inventory, forbidden warehouse.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ReceiptRtvCreateServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private AdjustmentRepository adjustmentRepository;
    @Mock
    private DebitNoteRepository debitNoteRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;
    @Mock
    private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private QuarantineRecordRepository quarantineRecordRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private AccountingPeriodService accountingPeriodService;

    private ReceiptValidationService receiptValidationService;
    private QuarantineRtvService receiptService;

    private User manager;
    private Warehouse warehouse;
    private Supplier supplier;
    private Receipt qcFailedReceipt;
    private ReceiptItem failedItem;
    private Product product;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(10L);

        supplier = new Supplier();
        supplier.setId(20L);

        manager = new User();
        manager.setId(5L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        product = new Product();
        product.setId(100L);

        qcFailedReceipt = new Receipt();
        qcFailedReceipt.setId(1L);
        qcFailedReceipt.setReceiptNumber("RCV-2026-QC-FAIL");
        qcFailedReceipt.setStatus(ReceiptStatus.QC_FAILED);
        qcFailedReceipt.setWarehouse(warehouse);
        qcFailedReceipt.setSupplier(supplier);
        qcFailedReceipt.setDocumentDate(LocalDate.now());
        qcFailedReceipt.setVersion(2);
        qcFailedReceipt.setCreatedAt(OffsetDateTime.now());
        qcFailedReceipt.setUpdatedAt(OffsetDateTime.now());

        failedItem = new ReceiptItem();
        failedItem.setId(10L);
        failedItem.setProduct(product);
        failedItem.setActualQty(20);
        // Fully QC-failed item: sampleFailedQty must match actualQty here since
        // quarantine inventory (and therefore the RTV quantity) is keyed off
        // sampleFailedQty, not actualQty.
        failedItem.setSampleFailedQty(20);
        failedItem.setUnitCost(BigDecimal.valueOf(50));
        failedItem.setQcResult(QcResult.FAILED);

        receiptValidationService = new ReceiptValidationService(receiptRepository, userWarehouseAssignmentRepository);
        receiptService = new QuarantineRtvService(
                receiptRepository,
                receiptItemRepository,
                adjustmentRepository,
                debitNoteRepository,
                inventoryRepository,
                receiptValidationService,
                auditLogService,
                quarantineRecordRepository,
                priceHistoryRepository,
                accountingPeriodService);
        lenient().when(accountingPeriodService.resolveOpenPeriod(any()))
                .thenReturn(AccountingPeriod.builder().id(1L).periodName("2026-07").build());
    }

    // -----------------------------------------------------------------------
    // Happy path: create RTV for QC_FAILED receipt
    // -----------------------------------------------------------------------

    @Test
    void createRtv_happyPath_createsPendingAdjustmentAndDebitNote() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Hàng bị lỗi ngoại quan — trả lại NCC");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                "RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR)).thenReturn(false);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(i -> {
            Adjustment adj = i.getArgument(0);
            adj.setId(100L);
            return adj;
        });
        when(debitNoteRepository.save(any(DebitNote.class))).thenAnswer(i -> {
            DebitNote dn = i.getArgument(0);
            dn.setId(200L);
            return dn;
        });

        RtvActionResponse response = receiptService.createRtv(1L, request, manager);

        assertNotNull(response);
        assertFalse(response.isConfirmed()); // Pending, not yet confirmed
        assertNotNull(response.getAdjustmentNumber());
        assertNotNull(response.getDebitNoteNumber());
        assertEquals(BigDecimal.valueOf(20), response.getQuarantineQty());

        // Verify adjustment created as pending (no approvedAt)
        verify(adjustmentRepository).save(argThat(adj -> adj.getType() == AdjustmentType.RETURN_TO_VENDOR
                && adj.getReferenceType().equals("RECEIPT")
                && adj.getReferenceId().equals(1L)
                && adj.getApprovedAt() == null // Still pending
        ));

        // Verify Debit Note auto-created
        verify(debitNoteRepository).save(argThat(dn -> dn.getSupplier().getId().equals(20L)
                && dn.getReceipt().getId().equals(1L)
                && dn.getFailedQty().compareTo(BigDecimal.valueOf(20)) == 0));
    }

    @Test
    void createRtv_happyPath_doesNotDeductQuarantineInventory() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Hàng lỗi");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(adjustmentRepository.save(any())).thenAnswer(i -> {
            Adjustment a = i.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(debitNoteRepository.save(any())).thenAnswer(i -> {
            DebitNote d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        receiptService.createRtv(1L, request, manager);

        // Creating RTV must NOT deduct quarantine inventory
        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).findByWarehouseProductBatchLocationForUpdate(any(), any(), any(), any());
    }

    @Test
    void createRtv_happyPath_auditLogContainsInventoryDeductedFalse() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lỗi");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(adjustmentRepository.save(any())).thenAnswer(i -> {
            Adjustment a = i.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(debitNoteRepository.save(any())).thenAnswer(i -> {
            DebitNote d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        receiptService.createRtv(1L, request, manager);

        verify(auditLogService).log(eq(manager), eq(AuditAction.QUARANTINE_RTV_CREATE),
                eq("ADJUSTMENT"), any(), any(), eq(10L), isNull(),
                argThat(newVal -> Boolean.FALSE.equals(((java.util.Map<?, ?>) newVal).get("inventoryDeducted"))));
    }

    // -----------------------------------------------------------------------
    // Duplicate RTV
    // -----------------------------------------------------------------------

    @Test
    void createRtv_duplicateRtv_throwsRtvAlreadyExists() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lý do");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                "RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR)).thenReturn(true); // Already exists

        assertThrows(RtvAlreadyExistsException.class,
                () -> receiptService.createRtv(1L, request, manager));

        verify(adjustmentRepository, never()).save(any());
        verify(debitNoteRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Non-QC_FAILED status
    // -----------------------------------------------------------------------

    @Test
    void createRtv_approvedStatus_throwsBusinessRuleViolation() {
        qcFailedReceipt.setStatus(ReceiptStatus.APPROVED);
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lý do");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.createRtv(1L, request, manager));

        assertTrue(ex.getMessage().contains("QC_FAILED"));
    }

    @Test
    void createRtv_qcCompletedStatus_throwsBusinessRuleViolation() {
        qcFailedReceipt.setStatus(ReceiptStatus.QC_COMPLETED);
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lý do");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));

        assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.createRtv(1L, request, manager));
    }

    // -----------------------------------------------------------------------
    // No items in quarantine
    // -----------------------------------------------------------------------

    @Test
    void createRtv_noItems_throwsBusinessRuleViolation() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lý do");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(any(), any(), any())).thenReturn(false);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of()); // Empty

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> receiptService.createRtv(1L, request, manager));

        assertTrue(ex.getMessage().contains("NO_QUARANTINE_ITEMS"));
    }

    // -----------------------------------------------------------------------
    // Forbidden warehouse
    // -----------------------------------------------------------------------

    @Test
    void createRtv_forbiddenWarehouse_throwsForbiddenReceiptWarehouse() {
        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Lý do");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L))
                .thenReturn(List.of(999L)); // Different warehouse

        assertThrows(ForbiddenReceiptWarehouseException.class,
                () -> receiptService.createRtv(1L, request, manager));
    }

    @Test
    void createRtv_returnReceiptApproved_createsPendingAdjustmentAndDebitNote() {
        qcFailedReceipt.setType(ReceiptType.RETURN);
        qcFailedReceipt.setStatus(ReceiptStatus.APPROVED);

        ReceiptRtvCreateRequest request = new ReceiptRtvCreateRequest();
        request.setExpectedVersion(2);
        request.setReason("Hàng lỗi trả từ đại lý — trả NCC");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(qcFailedReceipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(10L));
        when(adjustmentRepository.existsByReferenceTypeAndReferenceIdAndType(
                "RECEIPT", 1L, AdjustmentType.RETURN_TO_VENDOR)).thenReturn(false);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(failedItem));
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(i -> {
            Adjustment adj = i.getArgument(0);
            adj.setId(100L);
            return adj;
        });
        when(debitNoteRepository.save(any(DebitNote.class))).thenAnswer(i -> {
            DebitNote dn = i.getArgument(0);
            dn.setId(200L);
            return dn;
        });

        RtvActionResponse response = receiptService.createRtv(1L, request, manager);

        assertNotNull(response);
        assertFalse(response.isConfirmed());
        assertNotNull(response.getAdjustmentNumber());
        assertNotNull(response.getDebitNoteNumber());
        assertEquals(BigDecimal.valueOf(20), response.getQuarantineQty());
    }
}
