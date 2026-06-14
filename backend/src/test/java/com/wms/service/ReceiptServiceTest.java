package com.wms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.ReceiveReceiptItemRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.DocumentSequence;
import com.wms.entity.Product;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
import com.wms.entity.Supplier;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.QcResult;
import com.wms.enums.QcSamplingMethod;
import com.wms.enums.ReceiptSourceChannel;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
import com.wms.enums.UserRole;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ReceiptCountException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.mapper.ReceiptMapper;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.SupplierRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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

    private ReceiptService receiptService;
    private User planner;
    private User warehouseStaff;
    private Supplier supplier;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        receiptService = new ReceiptService(sequenceRepository, receiptRepository, receiptItemRepository,
                supplierRepository, warehouseRepository, productRepository,
                assignmentRepository, auditLogService, new ReceiptMapper());
        planner = user(1L, UserRole.PLANNER);
        warehouseStaff = user(2L, UserRole.WAREHOUSE_STAFF);
        supplier = supplier(10L, true);
        warehouse = warehouse(20L, true);
        product = product(30L, true);
    }

    @Test
    void createPurchaseReceipt_successPersistsAndAudits() {
        stubValidLookups();
        when(sequenceRepository.findBySequenceKeyForUpdate("RECEIPT"))
                .thenReturn(Optional.of(sequence()));
        when(receiptRepository.saveAndFlush(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt receipt = invocation.getArgument(0);
            receipt.setId(100L);
            return receipt;
        });
        when(receiptItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReceiptResponse response = receiptService.createPurchaseReceipt(validRequest(), planner);

        assertEquals(100L, response.getId());
        assertEquals("PURCHASE", response.getType());
        assertEquals("PENDING_RECEIPT", response.getStatus());
        assertEquals(500, response.getItems().get(0).getExpectedQty());
        assertNotNull(response.getReceiptNumber());
        verify(auditLogService).log(eq(planner), eq(AuditAction.CREATE),
                eq("RECEIPT"), eq(100L), any(), eq(20L), eq(null), any());
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
    void createPurchaseReceipt_rejectsDuplicateSourceReference() {
        stubHeaderLookups();
        when(receiptRepository.existsBySupplierIdAndWarehouseIdAndSourceOrderCodeAndTypeAndStatusNot(
                10L, 20L, "PO-1", ReceiptType.PURCHASE, ReceiptStatus.REJECTED))
                .thenReturn(true);

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
        verify(auditLogService).log(eq(warehouseStaff), eq(AuditAction.UPDATE),
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
    void receiveReceiptCounts_rejectsNonPositiveCount() {
        Receipt receipt = receipt(100L, ReceiptStatus.PENDING_RECEIPT);
        ReceiptItem item1 = item(501L, receipt, 30L, 100);
        stubReceive(receipt, List.of(item1));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 0)), warehouseStaff));

        assertEquals("INVALID_RECEIPT_COUNT", ex.getCode());
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
        Receipt rejected = receipt(100L, ReceiptStatus.REJECTED);
        when(receiptRepository.findByIdWithWarehouse(100L)).thenReturn(Optional.of(rejected));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));

        ReceiptCountException ex = assertThrows(ReceiptCountException.class,
                () -> receiptService.receiveReceiptCounts(100L,
                        receiveRequest(line(501L, 1)), warehouseStaff));

        assertEquals("RECEIPT_ALREADY_FINALIZED", ex.getCode());
        verify(receiptItemRepository, never()).findByReceiptIdOrderByIdAsc(any());
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
        verify(receiptRepository, never()).findByIdWithWarehouse(any());
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

    private void stubValidLookups() {
        stubHeaderLookups();
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
    }

    private void stubHeaderLookups() {
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
    }

    private CreateReceiptRequest validRequest() {
        CreateReceiptItemRequest item = new CreateReceiptItemRequest();
        item.setProductId(30L);
        item.setExpectedQty(500);

        CreateReceiptRequest request = new CreateReceiptRequest();
        request.setSupplierId(10L);
        request.setWarehouseId(20L);
        request.setContactPerson("Nguyen Van A");
        request.setSourceReference("PO-1");
        request.setSourceChannel(ReceiptSourceChannel.ZALO);
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
        request.setItems(List.of(lines));
        return request;
    }

    private void stubReceive(Receipt receipt, List<ReceiptItem> items) {
        when(receiptRepository.findByIdWithWarehouse(receipt.getId()))
                .thenReturn(Optional.of(receipt));
        when(assignmentRepository.findWarehouseIdsByUserId(2L)).thenReturn(List.of(20L));
        when(receiptItemRepository.findByReceiptIdOrderByIdAsc(receipt.getId()))
                .thenReturn(items);
    }

    private void stubReceiveSaves() {
        when(receiptItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
        sequence.setSequenceKey("RECEIPT");
        sequence.setNextValue(1L);
        return sequence;
    }
}
