package com.wms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.DocumentSequence;
import com.wms.entity.Product;
import com.wms.entity.Receipt;
import com.wms.entity.ReceiptItem;
import com.wms.entity.Supplier;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.ReceiptSourceChannel;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.ReceiptType;
import com.wms.enums.UserRole;
import com.wms.exception.DuplicateResourceException;
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
    private Supplier supplier;
    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        receiptService = new ReceiptService(sequenceRepository, receiptRepository, receiptItemRepository,
                supplierRepository, warehouseRepository, productRepository,
                assignmentRepository, auditLogService, new ReceiptMapper());
        planner = user(1L, UserRole.PLANNER);
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
