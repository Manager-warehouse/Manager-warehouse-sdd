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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.PreReceiveApprovalRequest;
import com.wms.dto.request.ReceiptCancelRequest;
import com.wms.dto.request.ReceiptReopenRequest;
import com.wms.dto.request.ReceiveQcReceiptItemRequest;
import com.wms.dto.request.ReceiveQcReceiptRequest;
import com.wms.dto.request.ReceiveReceiptItemRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.request.ReviseReceiptItemRequest;
import com.wms.dto.request.ReviseReceiptRequest;
import com.wms.dto.request.StorekeeperReviewRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.stock_receiving.QcResult;
import com.wms.enums.stock_receiving.QcSamplingMethod;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.enums.stock_receiving.ReceiptType;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ReceiptCountException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.mapper.ReceiptMapper;
import com.wms.repository.CreditNoteRepository;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.stock_receiving.ReceiptItemRepository;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.price_management.PriceHistoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private DocumentSequenceRepository sequenceRepository;
    @Mock
    private ReceiptRepository receiptRepository;
    @Mock
    private ReceiptItemRepository receiptItemRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private AccountingPeriodService accountingPeriodService;
    @Mock
    private CreditNoteRepository creditNoteRepository;
    @Mock
    private PriceHistoryService priceHistoryService;

    private ReceiptService receiptService;
    private User planner;
    private User warehouseStaff;
    private User warehouseManager;
    private User storekeeper;
    private Supplier supplier;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        receiptService = new ReceiptService(sequenceRepository, receiptRepository, receiptItemRepository,
                supplierRepository, warehouseRepository, productRepository,
                assignmentRepository, auditLogService, new ReceiptMapper(), accountingPeriodService,
                creditNoteRepository, priceHistoryService);
        planner = user(1L, UserRole.PLANNER);
        warehouseStaff = user(2L, UserRole.WAREHOUSE_STAFF);
        warehouseManager = user(3L, UserRole.WAREHOUSE_MANAGER);
        storekeeper = user(4L, UserRole.STOREKEEPER);
        supplier = supplier(10L, true);
        warehouse = warehouse(20L, true);
        product = product(30L, true);
    }

    @Test
    void createPurchaseReceipt_successPersistsAndAudits() {
        stubValidLookups();
        when(sequenceRepository.findBySequenceKeyForUpdate("RECEIPT-20260728"))
                .thenReturn(Optional.of(sequence()));
        when(accountingPeriodService.resolveOpenPeriod(any()))
                .thenReturn(AccountingPeriod.builder().id(1L).periodName("2026-07").build());
        when(receiptRepository.saveAndFlush(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt receipt = invocation.getArgument(0);
            receipt.setId(100L);
            return receipt;
        });
        when(receiptItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(priceHistoryService.lookupApproved(30L, 20L, LocalDate.of(2026, 7, 28)))
                .thenReturn(Optional.of(price(30L, new BigDecimal("125000"))));

        ReceiptResponse response = receiptService.createPurchaseReceipt(validRequest(), planner);

        assertEquals(100L, response.getId());
        assertEquals("PURCHASE", response.getType());
        assertEquals("PENDING_MANAGER_APPROVAL", response.getStatus());
        assertEquals(500, response.getItems().get(0).getExpectedQty());
        assertEquals(0, new BigDecimal("125000").compareTo(response.getItems().get(0).getUnitCost()));
        assertEquals("PO-20260728-0001", response.getReceiptNumber());
        assertEquals(LocalDate.of(2026, 7, 28), response.getDocumentDate());
        ArgumentCaptor<Map<String, Object>> afterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).log(eq(planner), eq(AuditAction.RECEIPT_CREATE),
                eq("RECEIPT"), eq(100L), eq("PO-20260728-0001"), eq(20L), eq(null),
                afterCaptor.capture());
        assertEquals("PO-20260728-0001", afterCaptor.getValue().get("receiptNumber"));
        assertEquals(LocalDate.of(2026, 7, 28), afterCaptor.getValue().get("documentDate"));
    }

    @Test
    void createPurchaseReceipt_rejectsInactiveSupplier() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, false)));

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void createPurchaseReceipt_rejectsInactiveProduct() {
        stubHeaderLookups();
        when(sequenceRepository.findBySequenceKeyForUpdate("RECEIPT-20260728"))
                .thenReturn(Optional.of(sequence()));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product(30L, false)));

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));
    }

    @Test
    void createPurchaseReceipt_rejectsInactiveWarehouse() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse(20L, false)));

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));
    }

    @Test
    void createPurchaseReceipt_rejectsUnauthorizedWarehouse() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(99L));

        assertThrows(AccessDeniedException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));
    }

    @Test
    void createPurchaseReceipt_rejectsNonPositiveExpectedQty() {
        stubHeaderLookups();
        CreateReceiptRequest request = validRequest();
        request.getItems().get(0).setExpectedQty(0);

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.createPurchaseReceipt(request, planner));
    }

    @Test
    void createPurchaseReceipt_rejectsReceiptNumberConflict() {
        stubHeaderLookups();
        when(sequenceRepository.findBySequenceKeyForUpdate("RECEIPT-20260728"))
                .thenReturn(Optional.of(sequence()));
        when(accountingPeriodService.resolveOpenPeriod(any()))
                .thenReturn(AccountingPeriod.builder().id(1L).periodName("2026-07").build());
        when(receiptRepository.saveAndFlush(any(Receipt.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate receipt_number"));

        assertThrows(DuplicateResourceException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));
    }

    @Test
    void receiveReceiptCounts_successCalculatesShortAndOverCounts() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem shortItem = item(501L, receipt, 30L, 100);
        ReceiptItem overItem = item(502L, receipt, 31L, 100);
        ReceiptItem equalItem = item(503L, receipt, 32L, 100);
        stubReceive(receipt, List.of(shortItem, overItem, equalItem));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 90), line(502L, 120), line(503L, 100)), warehouseStaff);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(90, shortItem.getActualQty());
        assertEquals(0, shortItem.getOverReceivedQty());
        assertEquals(100, overItem.getActualQty());
        assertEquals(20, overItem.getOverReceivedQty());
        assertEquals(100, equalItem.getActualQty());
        assertEquals(0, equalItem.getOverReceivedQty());
        verify(auditLogService).log(eq(warehouseStaff), eq(AuditAction.RECEIPT_RECEIVE),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L),
                any(Map.class), any(Map.class));
    }

    @Test
    void receiveReceiptCounts_below30Percent_staysDraftForQcReview() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 20)), warehouseStaff);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(20, item1.getActualQty());
        assertNull(receipt.getRejectionReason());
        verify(auditLogService).log(eq(warehouseStaff), eq(AuditAction.RECEIPT_RECEIVE),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L),
                any(Map.class), any(Map.class));
    }

    @Test
    void receiveReceiptCounts_rejectsMissingItemWithoutPartialChanges() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        ReceiptItem item2 = item(502L, receipt, 31L, 100);
        stubReceive(receipt, List.of(item1, item2));

        assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 90)), warehouseStaff));
        assertNull(item1.getActualQty());
        assertNull(item2.getActualQty());
        verify(receiptItemRepository, never()).saveAll(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void receiveReceiptCounts_rejectsDuplicateItemId() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 90), line(501L, 91)), warehouseStaff));

        assertEquals("INVALID_RECEIPT_COUNT", ex.getCode());
    }

    @Test
    void receiveReceiptCounts_rejectsItemFromAnotherReceipt() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(999L, 90)), warehouseStaff));

        assertEquals("INVALID_RECEIPT_COUNT", ex.getCode());
    }

    @Test
    void receiveReceiptCounts_allowsZeroCount() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 0)), warehouseStaff);

        assertEquals("DRAFT", response.getStatus());
        assertEquals(0, item1.getActualQty());
        assertEquals(0, item1.getOverReceivedQty());
    }

    @Test
    void receiveReceiptCounts_rejectsNegativeCount() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, -1)), warehouseStaff));

        assertEquals("INVALID_RECEIPT_COUNT", ex.getCode());
    }

    @Test
    void receiveReceiptCounts_rejectsApprovedReceipt() {
        Receipt approved = receipt(100L, ReceiptStatus.APPROVED);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(approved));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), warehouseStaff));

        assertEquals("RECEIPT_ALREADY_FINALIZED", ex.getCode());
        verify(receiptItemRepository, never()).findByReceiptIdOrderByIdAsc(any());
    }

    @Test
    void receiveReceiptCounts_rejectsRejectedReceipt() {
        Receipt rejected = receipt(100L, ReceiptStatus.RETURNED_TO_SUPPLIER);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(rejected));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), warehouseStaff));

        assertEquals("RECEIPT_ALREADY_FINALIZED", ex.getCode());
        verify(receiptItemRepository, never()).findByReceiptIdOrderByIdAsc(any());
    }

    @Test
    void getReceiptById_allowsAccountantWithoutWarehouseAssignment() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        when(receiptRepository.findByIdWithSupplierAndWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());
        User accountant = user(3L, UserRole.ACCOUNTANT);

        ReceiptResponse response = receiptService.getReceiptById(100L, accountant);

        assertEquals(100L, response.getId());
        verify(assignmentRepository, never()).findWarehouseIdsByUserId(any());
    }

    @Test
    void getReceiptsByWarehouseAndType_allowsAccountantManagerWithoutWarehouseAssignment() {
        when(receiptRepository.findByWarehouseIdOrderByDocumentDateDescCreatedAtDesc(20L))
                .thenReturn(List.of());
        User accountantManager = user(4L, UserRole.ACCOUNTANT_MANAGER);

        List<ReceiptResponse> response = receiptService.getReceiptsByWarehouseAndType(20L, null, accountantManager);

        assertEquals(0, response.size());
        verify(assignmentRepository, never()).findWarehouseIdsByUserId(any());
    }

    @Test
    void receiveReceiptCounts_rejectsUnauthorizedWarehouseStaff() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(99L));

        assertThrows(AccessDeniedException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), warehouseStaff));
    }

    @Test
    void receiveReceiptCounts_rejectsNonWarehouseStaffRole() {
        assertThrows(AccessDeniedException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), planner));
        assertThrows(AccessDeniedException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), storekeeper));
        verify(receiptRepository, never()).findByIdWithWarehouse(any());
    }

    @Test
    void createPurchaseReceipt_requiresApprovedAccountingPrice() {
        stubValidLookups();
        when(sequenceRepository.findBySequenceKeyForUpdate("RECEIPT-20260728"))
                .thenReturn(Optional.of(sequence()));
        when(accountingPeriodService.resolveOpenPeriod(any()))
                .thenReturn(AccountingPeriod.builder().id(1L).periodName("2026-07").build());
        when(receiptRepository.saveAndFlush(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(priceHistoryService.lookupApproved(30L, 20L, LocalDate.of(2026, 7, 28)))
                .thenReturn(Optional.empty());

        UnprocessableEntityException ex = assertThrows(UnprocessableEntityException.class,
                () -> receiptService.createPurchaseReceipt(validRequest(), planner));

        assertEquals(true, ex.getMessage().contains("APPROVED_PRICE_REQUIRED"));
        verify(receiptItemRepository, never()).saveAll(any());
    }

    @Test
    void receiveReceiptCounts_correctsDraftReceipt() {
        Receipt receipt = receipt(100L, ReceiptStatus.DRAFT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        item1.setActualQty(80);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 95)), warehouseStaff);

        assertEquals(ReceiptStatus.DRAFT, receipt.getStatus());
        assertEquals(95, item1.getActualQty());
    }

    @Test
    void receiveReceiptCounts_correctsQcCompletedAndClearsQcFields() {
        Receipt receipt = receipt(100L, ReceiptStatus.QC_COMPLETED);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        item1.setQcResult(QcResult.PASSED);
        item1.setQcSamplingMethod(QcSamplingMethod.FULL_INSPECTION);
        item1.setSampleQty(10);
        item1.setSamplePassedQty(10);
        item1.setSampleFailedQty(0);
        item1.setQcFailureReason("old");
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 95)), warehouseStaff);

        assertEquals(ReceiptStatus.DRAFT, receipt.getStatus());
        assertNull(item1.getQcResult());
        assertNull(item1.getQcSamplingMethod());
        assertNull(item1.getSampleQty());
        assertNull(item1.getSamplePassedQty());
        assertNull(item1.getSampleFailedQty());
        assertNull(item1.getQcFailureReason());
    }

    @Test
    void receiveReceiptCounts_correctsQcFailedAndReturnsToDraft() {
        Receipt receipt = receipt(100L, ReceiptStatus.QC_FAILED);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        item1.setQcResult(QcResult.FAILED);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        receiptService.receiveReceiptCounts(100L,
                receiveRequest(line(501L, 95)), warehouseStaff);

        assertEquals(ReceiptStatus.DRAFT, receipt.getStatus());
        assertNull(item1.getQcResult());
    }

    @Test
    void receiveAndQcReceipt_allPassTransitionsToStorekeeperReview() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.receiveAndQcReceipt(100L,
                receiveQcRequest(receiveQcLine(501L, 100, 100, 0, null)), warehouseStaff);

        assertEquals("PENDING_STOREKEEPER_REVIEW", response.getStatus());
        assertEquals(100, item1.getActualQty());
        assertEquals(100, item1.getQualityPassedQty());
        assertEquals(0, item1.getQualityFailedQty());
        assertEquals(0, item1.getOverReceivedQty());
        assertEquals(QcResult.PASSED, item1.getQcResult());
        verify(auditLogService).log(eq(warehouseStaff), eq(AuditAction.RECEIPT_RECEIVE_QC),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L),
                any(Map.class), any(Map.class));
    }

    @Test
    void receiveAndQcReceipt_partialFailedWaitsForStorekeeperReview() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        receiptService.receiveAndQcReceipt(100L,
                receiveQcRequest(receiveQcLine(501L, 98, 95, 3, "Damaged")), warehouseStaff);

        assertEquals(ReceiptStatus.PENDING_STOREKEEPER_REVIEW, receipt.getStatus());
        assertEquals(98, item1.getActualQty());
        assertEquals(95, item1.getQualityPassedQty());
        assertEquals(3, item1.getQualityFailedQty());
        assertEquals(0, item1.getQuarantineReadyQty());
        assertEquals("Damaged", item1.getQcFailureReason());
        assertEquals(QcResult.FAILED, item1.getQcResult());
    }

    @Test
    void reviewStorekeeperCountQc_approveAllPassTransitionsToQcCompleted() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_STOREKEEPER_REVIEW);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        item1.setActualQty(100);
        item1.setQualityPassedQty(100);
        item1.setQualityFailedQty(0);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.reviewStorekeeperCountQc(100L,
                storekeeperReviewRequest(StorekeeperReviewDecision.APPROVE, null), storekeeper);

        assertEquals("QC_COMPLETED", response.getStatus());
        assertEquals(ReceiptStatus.QC_COMPLETED, receipt.getStatus());
        assertEquals(storekeeper, receipt.getStorekeeperReviewedBy());
        assertNotNull(receipt.getStorekeeperReviewedAt());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.RECEIPT_STOREKEEPER_REVIEW_APPROVE),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L),
                any(Map.class), any(Map.class));
    }

    @Test
    void reviewStorekeeperCountQc_requestRecountTransitionsToRecountRequired() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_STOREKEEPER_REVIEW);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        ReceiptResponse response = receiptService.reviewStorekeeperCountQc(100L,
                storekeeperReviewRequest(StorekeeperReviewDecision.REQUEST_RECOUNT, "Count mismatch"), storekeeper);

        assertEquals("RECOUNT_REQUIRED", response.getStatus());
        assertEquals("Count mismatch", receipt.getRecountReason());
        assertEquals(storekeeper, receipt.getStorekeeperReviewedBy());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.RECEIPT_STOREKEEPER_RECOUNT_REQUEST),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L),
                any(Map.class), any(Map.class));
    }

    @Test
    void receiveAndQcReceipt_rejectsQcTotalMismatch() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveAndQcReceipt(100L,
                        receiveQcRequest(receiveQcLine(501L, 100, 90, 5, "Damaged")),
                        warehouseStaff));

        assertEquals("RECEIVE_QC_TOTAL_MISMATCH", ex.getCode());
        verify(receiptItemRepository, never()).saveAll(any());
    }

    @Test
    void receiveAndQcReceipt_requiresFailureReason() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveAndQcReceipt(100L,
                        receiveQcRequest(receiveQcLine(501L, 100, 95, 5, " ")),
                        warehouseStaff));

        assertEquals("QC_FAILURE_REASON_REQUIRED", ex.getCode());
    }

    @Test
    void receiveAndQcReceipt_recordsOverReceivedWithoutInventoryOrBatchReadiness() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));
        stubReceiveSaves();

        receiptService.receiveAndQcReceipt(100L,
                receiveQcRequest(receiveQcLine(501L, 120, 120, 0, null)), warehouseStaff);

        assertEquals(120, item1.getActualQty());
        assertEquals(20, item1.getOverReceivedQty());
        assertNull(item1.getBatch());
        assertNull(item1.getLocation());
        assertEquals(0, item1.getApprovedQty());
        assertEquals(0, item1.getQuarantineQty());
    }

    @Test
    void receiveAndQcReceipt_rejectsExpectedVersionMismatch() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        receipt.setVersion(2);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveAndQcReceipt(100L,
                        receiveQcRequest(receiveQcLine(501L, 100, 100, 0, null)),
                        warehouseStaff));

        assertEquals("INVENTORY_VERSION_CONFLICT", ex.getCode());
        verify(receiptItemRepository, never()).findByReceiptIdOrderByIdAsc(any());
    }

    @Test
    void receiveAndQcReceipt_blocksPendingManagerApprovalAndRevisionRequired() {
        for (ReceiptStatus status : List.of(ReceiptStatus.PENDING_MANAGER_APPROVAL,
                ReceiptStatus.REVISION_REQUIRED)) {
            Receipt receipt = receipt(100L, status);
            when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
            when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

            ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                    () -> receiptService.receiveAndQcReceipt(100L,
                            receiveQcRequest(receiveQcLine(501L, 100, 100, 0, null)),
                            warehouseStaff));

            assertEquals("RECEIPT_PENDING_MANAGER_APPROVAL", ex.getCode());
        }
    }

    @Test
    void cancelReceipt_successForDraftReceipt() {
        Receipt receipt = receipt(100L, ReceiptStatus.DRAFT);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User planner = user(7L, UserRole.PLANNER);
        when(assignmentRepository.findWarehouseIdsByUserId(7L)).thenReturn(List.of(20L));

        ReceiptResponse response = receiptService.cancelReceipt(100L,
                cancelRequest("User cancellation", 0), planner);

        assertEquals("CANCELLED", response.getStatus());
        assertEquals("User cancellation", response.getRejectionReason());
        assertEquals("User cancellation", response.getCancellationReason());
        verify(auditLogService).log(eq(planner), eq(AuditAction.RECEIPT_CANCEL),
                eq("RECEIPT"), eq(100L), any(), eq(20L), any(), any());
    }

    @Test
    void cancelReceipt_rejectsFinalizedReceipt() {
        Receipt receipt = receipt(100L, ReceiptStatus.PUTAWAY_COMPLETED);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        User planner = user(7L, UserRole.PLANNER);
        when(assignmentRepository.findWarehouseIdsByUserId(7L)).thenReturn(List.of(20L));

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.cancelReceipt(100L, cancelRequest("User cancellation", 0), planner));
    }

    @Test
    void reopenReceipt_successForApprovedReceiptBeforePutaway() {
        Receipt receipt = receipt(100L, ReceiptStatus.APPROVED);
        User manager = user(5L, UserRole.WAREHOUSE_MANAGER);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(20L));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptService.reopenReceipt(100L,
                reopenRequest("Reopen to draft", 0), manager);

        assertEquals("DRAFT", response.getStatus());
        verify(auditLogService).log(eq(manager), eq(AuditAction.RECEIPT_REOPEN),
                eq("RECEIPT"), eq(100L), any(), eq(20L), any(), any());
    }

    @Test
    void reopenReceipt_rejectsReceiptAfterPutaway() {
        Receipt receipt = receipt(100L, ReceiptStatus.APPROVED);
        receipt.setPutawayCompletedAt(java.time.OffsetDateTime.now());
        User manager = user(5L, UserRole.WAREHOUSE_MANAGER);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(5L)).thenReturn(List.of(20L));

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.reopenReceipt(100L, reopenRequest("Reopen after putaway", 0), manager));
    }

    private void stubValidLookups() {
        stubHeaderLookups();
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
    }

    private void stubHeaderLookups() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
    }

    @Test
    void decidePreReceiveApproval_approveMovesReceiptToPendingReceipt() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_MANAGER_APPROVAL);
        ReceiptItem item = item(501L, receipt, 30L, 100);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(item));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptService.decidePreReceiveApproval(100L,
                preReceiveRequest("APPROVE", null), warehouseManager);

        assertEquals("PENDING_RECEIPT", response.getStatus());
        assertEquals(warehouseManager, receipt.getPreReceiveApprovedBy());
        assertNull(receipt.getPreReceiveRejectionReason());
        verify(auditLogService).log(eq(warehouseManager), eq(AuditAction.RECEIPT_PRE_RECEIVE_APPROVE),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L), any(Map.class), any(Map.class));
    }

    @Test
    void decidePreReceiveApproval_approvesLegacyPendingReceiptWithoutCount() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item = item(501L, receipt, 30L, 100);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(item));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptService.decidePreReceiveApproval(100L,
                preReceiveRequest("APPROVE", null), warehouseManager);

        assertEquals("PENDING_RECEIPT", response.getStatus());
        assertEquals(warehouseManager, receipt.getPreReceiveApprovedBy());
        assertNotNull(receipt.getPreReceiveApprovedAt());
        verify(auditLogService).log(eq(warehouseManager), eq(AuditAction.RECEIPT_PRE_RECEIVE_APPROVE),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L), any(Map.class), any(Map.class));
    }

    @Test
    void decidePreReceiveApproval_rejectRequiresReason() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_MANAGER_APPROVAL);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        assertThrows(UnprocessableEntityException.class,
                () -> receiptService.decidePreReceiveApproval(100L,
                        preReceiveRequest("REJECT", " "), warehouseManager));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void decidePreReceiveApproval_rejectsAdminRole() {
        User admin = user(9L, UserRole.ADMIN);

        assertThrows(AccessDeniedException.class,
                () -> receiptService.decidePreReceiveApproval(100L,
                        preReceiveRequest("APPROVE", null), admin));
        verify(receiptRepository, never()).findByIdWithWarehouse(any());
    }

    @Test
    void decidePreReceiveApproval_rejectMovesReceiptToRevisionRequired() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_MANAGER_APPROVAL);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(3L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptService.decidePreReceiveApproval(100L,
                preReceiveRequest("REJECT", "Need source document"), warehouseManager);

        assertEquals("REVISION_REQUIRED", response.getStatus());
        assertEquals("Need source document", receipt.getPreReceiveRejectionReason());
        verify(auditLogService).log(eq(warehouseManager), eq(AuditAction.RECEIPT_PRE_RECEIVE_REJECT),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L), any(Map.class), any(Map.class));
    }

    @Test
    void reviseReceipt_resubmitsRevisionRequiredToManagerApproval() {
        Receipt receipt = receipt(100L, ReceiptStatus.REVISION_REQUIRED);
        ReceiptItem item = item(501L, receipt, 30L, 100);
        receipt.setPreReceiveRejectionReason("Need source document");
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(item));
        when(accountingPeriodService.resolveOpenPeriod(LocalDate.of(2026, 7, 29)))
                .thenReturn(AccountingPeriod.builder().id(2L).periodName("2026-07").build());
        when(productRepository.findById(31L)).thenReturn(Optional.of(product(31L, true)));
        when(priceHistoryService.lookupApproved(31L, 20L, LocalDate.of(2026, 7, 29)))
                .thenReturn(Optional.of(price(31L, new BigDecimal("99000"))));
        when(receiptItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(inv -> inv.getArgument(0));

        ReceiptResponse response = receiptService.reviseReceipt(100L, revisionRequest(), planner);

        assertEquals("PENDING_MANAGER_APPROVAL", response.getStatus());
        assertNull(receipt.getPreReceiveRejectionReason());
        assertEquals(31L, item.getProduct().getId());
        assertEquals(200, item.getExpectedQty());
        assertEquals(0, new BigDecimal("99000").compareTo(item.getUnitCost()));
        verify(auditLogService).log(eq(planner), eq(AuditAction.RECEIPT_PRE_RECEIVE_RESUBMIT),
                eq("RECEIPT"), eq(100L), eq("RN-1"), eq(20L), any(Map.class), any(Map.class));
    }

    @Test
    void receiveReceiptCounts_blocksPreReceiveApprovalStates() {
        Receipt pending = receipt(100L, ReceiptStatus.PENDING_MANAGER_APPROVAL);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(pending));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        ReceiptCountException pendingEx = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), warehouseStaff));
        assertEquals("RECEIPT_PENDING_MANAGER_APPROVAL", pendingEx.getCode());

        Receipt revision = receipt(101L, ReceiptStatus.REVISION_REQUIRED);
        when(receiptRepository.findByIdWithWarehouse(101L)).thenReturn(Optional.of(revision));
        ReceiptCountException revisionEx = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(101L,
                        receiveRequest(line(501L, 1)), warehouseStaff));
        assertEquals("RECEIPT_PENDING_MANAGER_APPROVAL", revisionEx.getCode());
        verify(receiptItemRepository, never()).findByReceiptIdOrderByIdAsc(any());
    }

    private ReceiptCancelRequest cancelRequest(String reason, int expectedVersion) {
        ReceiptCancelRequest request = new ReceiptCancelRequest();
        request.setReason(reason);
        request.setExpectedVersion(expectedVersion);
        return request;
    }

    private ReceiptReopenRequest reopenRequest(String reason, int expectedVersion) {
        ReceiptReopenRequest request = new ReceiptReopenRequest();
        request.setReason(reason);
        request.setExpectedVersion(expectedVersion);
        return request;
    }

    private PreReceiveApprovalRequest preReceiveRequest(String decision, String reason) {
        PreReceiveApprovalRequest request = new PreReceiveApprovalRequest();
        request.setExpectedVersion(0);
        request.setDecision(decision);
        request.setReason(reason);
        return request;
    }

    private ReviseReceiptRequest revisionRequest() {
        ReviseReceiptItemRequest item = new ReviseReceiptItemRequest();
        item.setReceiptItemId(501L);
        item.setProductId(31L);
        item.setExpectedQty(200);

        ReviseReceiptRequest request = new ReviseReceiptRequest();
        request.setExpectedVersion(0);
        request.setDocumentDate(LocalDate.of(2026, 7, 29));
        request.setItems(List.of(item));
        request.setNotes("Resubmitted");
        return request;
    }

    private CreateReceiptRequest validRequest() {
        CreateReceiptItemRequest item = new CreateReceiptItemRequest();
        item.setProductId(30L);
        item.setExpectedQty(500);
        item.setUnitCost(new BigDecimal("1.00"));

        CreateReceiptRequest request = new CreateReceiptRequest();
        request.setSupplierId(10L);
        request.setWarehouseId(20L);
        request.setDocumentDate(LocalDate.of(2026, 7, 28));
        request.setItems(List.of(item));
        return request;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Receipt receipt(Long id, ReceiptStatus status) {
        Receipt receipt = new Receipt();
        receipt.setId(id);
        receipt.setReceiptNumber("RN-1");
        receipt.setType(ReceiptType.PURCHASE);
        receipt.setStatus(status);
        receipt.setSupplier(supplier);
        receipt.setWarehouse(warehouse);
        return receipt;
    }

    private ReceiptItem item(Long id, Receipt receipt, Long productId, int expectedQty) {
        ReceiptItem item = new ReceiptItem();
        item.setId(id);
        item.setReceipt(receipt);
        item.setProduct(product(productId, true));
        item.setExpectedQty(expectedQty);
        item.setOverReceivedQty(0);
        return item;
    }

    private ReceiveReceiptItemRequest line(Long itemId, Integer countedQty) {
        ReceiveReceiptItemRequest line = new ReceiveReceiptItemRequest();
        line.setReceiptItemId(itemId);
        line.setCountedQty(countedQty);
        return line;
    }

    private ReceiveReceiptRequest receiveRequest(ReceiveReceiptItemRequest... lines) {
        ReceiveReceiptRequest request = new ReceiveReceiptRequest();
        request.setExpectedVersion(0);
        request.setItems(List.of(lines));
        return request;
    }

    private PriceHistory price(Long productId, BigDecimal costPrice) {
        return PriceHistory.builder()
                .id(productId)
                .product(product(productId, true))
                .warehouse(warehouse)
                .costPrice(costPrice)
                .sellingPrice(costPrice.multiply(BigDecimal.TEN))
                .effectiveDate(LocalDate.of(2026, 7, 1))
                .build();
    }

    private ReceiveQcReceiptItemRequest receiveQcLine(Long itemId,
                                                      Integer actualQty,
                                                      Integer passedQty,
                                                      Integer failedQty,
                                                      String failureReason) {
        ReceiveQcReceiptItemRequest line = new ReceiveQcReceiptItemRequest();
        line.setReceiptItemId(itemId);
        line.setActualQty(actualQty);
        line.setQualityPassedQty(passedQty);
        line.setQualityFailedQty(failedQty);
        line.setQcFailureReason(failureReason);
        return line;
    }

    private ReceiveQcReceiptRequest receiveQcRequest(ReceiveQcReceiptItemRequest... lines) {
        ReceiveQcReceiptRequest request = new ReceiveQcReceiptRequest();
        request.setExpectedVersion(0);
        request.setItems(List.of(lines));
        return request;
    }

    private StorekeeperReviewRequest storekeeperReviewRequest(StorekeeperReviewDecision decision, String reason) {
        StorekeeperReviewRequest request = new StorekeeperReviewRequest();
        request.setExpectedVersion(0);
        request.setDecision(decision);
        request.setReason(reason);
        return request;
    }

    private void stubReceive(Receipt receipt, List<ReceiptItem> items) {
        when(receiptRepository.findByIdWithWarehouse(receipt.getId()))
                .thenReturn(Optional.of(receipt));
        lenient().when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        lenient().when(assignmentRepository.findWarehouseIdsByUserId(4L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(receipt.getId()))
                .thenReturn(items);
    }

    private void stubReceiveSaves() {
        lenient().when(receiptItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Supplier supplier(Long id, boolean active) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setIsActive(active);
        return supplier;
    }

    private Warehouse warehouse(Long id, boolean active) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setIsActive(active);
        return warehouse;
    }

    private Product product(Long id, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setIsActive(active);
        return product;
    }

    private DocumentSequence sequence() {
        DocumentSequence sequence = new DocumentSequence();
        sequence.setSequenceKey("RECEIPT-20260728");
        sequence.setNextValue(1L);
        return sequence;
    }
}
