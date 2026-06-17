package com.wms.service;

import com.wms.dto.request.*;
import com.wms.dto.response.ReturnCreditNoteResponse;
import com.wms.entity.*;
import com.wms.enums.*;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private AuditLogService auditLogService;

    private ReturnService returnService;

    private User storekeeper;
    private User manager;
    private User accountant;
    private Warehouse warehouse;
    private Dealer dealer;
    private Product product;
    private DeliveryOrder deliveryOrder;
    private DeliveryOrderItem doItem;
    private AccountingPeriod openPeriod;

    @BeforeEach
    void setUp() {
        returnService = new ReturnService(
                receiptRepository,
                receiptItemRepository,
                deliveryOrderRepository,
                dealerRepository,
                warehouseRepository,
                productRepository,
                batchRepository,
                warehouseLocationRepository,
                inventoryRepository,
                creditNoteRepository,
                userWarehouseAssignmentRepository,
                accountingPeriodRepository,
                deliveryOrderItemRepository,
                auditLogService
        );

        storekeeper = new User();
        storekeeper.setId(20L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        manager = new User();
        manager.setId(21L);
        manager.setRole(UserRole.WAREHOUSE_MANAGER);

        accountant = new User();
        accountant.setId(22L);
        accountant.setRole(UserRole.ACCOUNTANT);

        warehouse = new Warehouse();
        warehouse.setId(1L);

        dealer = new Dealer();
        dealer.setId(5L);
        dealer.setCode("DEALER_A");
        dealer.setCurrentBalance(new BigDecimal("50000000.00")); // 50M VND

        product = new Product();
        product.setId(100L);

        doItem = new DeliveryOrderItem();
        doItem.setId(301L);
        doItem.setProduct(product);
        doItem.setIssuedQty(new BigDecimal("10.0"));
        doItem.setUnitPrice(new BigDecimal("1500000.00")); // 1.5M VND per unit

        deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(300L);
        deliveryOrder.setDoNumber("DO-001");
        deliveryOrder.setStatus(DeliveryOrderStatus.DELIVERED);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(10L);
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);
    }

    @Test
    void createReturnReceipt_happyPath_createsReceiptSuccessfully() {
        ReturnCreateRequest request = new ReturnCreateRequest();
        request.setWarehouseId(1L);
        request.setDealerId(5L);
        request.setDeliveryOrderId(300L);
        request.setContactPerson("Nguyen Van A");
        request.setNotes("Return due to minor scratch");

        ReturnItemRequest itemReq = new ReturnItemRequest();
        itemReq.setProductId(100L);
        itemReq.setExpectedQty(new BigDecimal("2.0"));
        request.setItems(List.of(itemReq));

        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(20L)).thenReturn(List.of(1L));
        when(accountingPeriodRepository.findPeriodByDateAndStatus(any(), eq(AccountingPeriodStatus.OPEN)))
                .thenReturn(Optional.of(openPeriod));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(dealerRepository.findById(5L)).thenReturn(Optional.of(dealer));
        when(deliveryOrderRepository.findById(300L)).thenReturn(Optional.of(deliveryOrder));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        Receipt receipt = returnService.createReturnReceipt(request, storekeeper);

        assertNotNull(receipt);
        assertEquals(ReceiptStatus.DRAFT, receipt.getStatus());
        assertEquals(ReceiptType.RETURN, receipt.getType());
        assertEquals("Nguyen Van A", receipt.getContactPerson());

        verify(receiptRepository, times(1)).save(any(Receipt.class));
        verify(receiptItemRepository, times(1)).save(any(ReceiptItem.class));
        verify(auditLogService, times(1)).log(eq(storekeeper), eq(AuditAction.CREATE), eq("RECEIPT"), any(), any(), eq(1L), any(), any());
    }

    @Test
    void createReturnReceipt_invalidDoStatus_throwsBusinessRuleViolation() {
        deliveryOrder.setStatus(DeliveryOrderStatus.PICKING); // not delivered

        ReturnCreateRequest request = new ReturnCreateRequest();
        request.setWarehouseId(1L);
        request.setDealerId(5L);
        request.setDeliveryOrderId(300L);

        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(20L)).thenReturn(List.of(1L));
        when(accountingPeriodRepository.findPeriodByDateAndStatus(any(), eq(AccountingPeriodStatus.OPEN)))
                .thenReturn(Optional.of(openPeriod));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(dealerRepository.findById(5L)).thenReturn(Optional.of(dealer));
        when(deliveryOrderRepository.findById(300L)).thenReturn(Optional.of(deliveryOrder));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () -> {
            returnService.createReturnReceipt(request, storekeeper);
        });

        assertTrue(ex.getMessage().contains("Can only return items from DELIVERED delivery orders"));
    }

    @Test
    void submitQc_happyPath_setsQcResults() {
        Receipt receipt = new Receipt();
        receipt.setId(1000L);
        receipt.setReceiptNumber("RET-001");
        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setWarehouse(warehouse);
        receipt.setDeliveryOrder(deliveryOrder);
        receipt.setVersion(1);

        ReceiptItem dbItem = new ReceiptItem();
        dbItem.setId(1001L);
        dbItem.setProduct(product);
        dbItem.setExpectedQty(5);

        ReturnQcRequest request = new ReturnQcRequest();
        request.setExpectedVersion(1);

        ReturnQcItemRequest itemQc = new ReturnQcItemRequest();
        itemQc.setReceiptItemId(1001L);
        itemQc.setActualQty(new BigDecimal("5.0"));
        itemQc.setQcPassedQty(new BigDecimal("4.0"));
        itemQc.setQcFailedQty(new BigDecimal("1.0"));
        itemQc.setQcFailureReason("1 unit chipped");
        request.setItems(List.of(itemQc));

        when(receiptRepository.findById(1000L)).thenReturn(Optional.of(receipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(20L)).thenReturn(List.of(1L));
        when(receiptItemRepository.findByReceiptId(1000L)).thenReturn(List.of(dbItem));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(300L)).thenReturn(List.of(doItem));

        Receipt result = returnService.submitQc(1000L, request, storekeeper);

        assertNotNull(result);
        assertEquals(ReceiptStatus.QC_COMPLETED, result.getStatus());
        assertEquals(5, dbItem.getActualQty());
        assertEquals(4, dbItem.getSamplePassedQty());
        assertEquals(1, dbItem.getSampleFailedQty());
        assertEquals(QcResult.PARTIAL, dbItem.getQcResult());

        verify(receiptItemRepository, times(1)).save(dbItem);
        verify(receiptRepository, times(1)).save(receipt);
    }

    @Test
    void submitQc_exceedDoQuantity_throwsBusinessRuleViolation() {
        Receipt receipt = new Receipt();
        receipt.setId(1000L);
        receipt.setStatus(ReceiptStatus.DRAFT);
        receipt.setWarehouse(warehouse);
        receipt.setDeliveryOrder(deliveryOrder); // Original DO quantity for product 100 is 10
        receipt.setVersion(1);

        ReceiptItem dbItem = new ReceiptItem();
        dbItem.setId(1001L);
        dbItem.setProduct(product);

        ReturnQcRequest request = new ReturnQcRequest();
        request.setExpectedVersion(1);

        ReturnQcItemRequest itemQc = new ReturnQcItemRequest();
        itemQc.setReceiptItemId(1001L);
        itemQc.setActualQty(new BigDecimal("12.0")); // exceeds 10!
        itemQc.setQcPassedQty(new BigDecimal("12.0"));
        itemQc.setQcFailedQty(BigDecimal.ZERO);
        request.setItems(List.of(itemQc));

        when(receiptRepository.findById(1000L)).thenReturn(Optional.of(receipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(20L)).thenReturn(List.of(1L));
        when(receiptItemRepository.findByReceiptId(1000L)).thenReturn(List.of(dbItem));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(300L)).thenReturn(List.of(doItem));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, () -> {
            returnService.submitQc(1000L, request, storekeeper);
        });

        assertTrue(ex.getMessage().contains("exceeds original issued quantity"));
    }

    @Test
    void approveReturn_happyPath_resolvesBatchAndApproves() {
        Receipt receipt = new Receipt();
        receipt.setId(1000L);
        receipt.setStatus(ReceiptStatus.QC_COMPLETED);
        receipt.setWarehouse(warehouse);
        receipt.setDocumentDate(LocalDate.now());
        receipt.setVersion(1);
        receipt.setReceiptNumber("RET-001");

        ReceiptItem item = new ReceiptItem();
        item.setId(1001L);
        item.setProduct(product);
        item.setActualQty(5);

        ReceiptDecisionRequest request = new ReceiptDecisionRequest();
        request.setExpectedVersion(1);

        Batch batch = new Batch();
        batch.setId(200L);

        when(receiptRepository.findById(1000L)).thenReturn(Optional.of(receipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(21L)).thenReturn(List.of(1L));
        when(receiptItemRepository.findByReceiptId(1000L)).thenReturn(List.of(item));
        when(batchRepository.findByProductWarehouseAndReceivedDate(100L, 1L, receipt.getDocumentDate()))
                .thenReturn(Optional.of(batch));

        Receipt result = returnService.approveReturn(1000L, request, manager);

        assertNotNull(result);
        assertEquals(ReceiptStatus.APPROVED, result.getStatus());
        assertEquals(batch, item.getBatch());

        verify(receiptItemRepository, times(1)).save(item);
        verify(receiptRepository, times(1)).save(receipt);
    }

    @Test
    void completePutaway_happyPath_updatesInventoryCorrectly() {
        Receipt receipt = new Receipt();
        receipt.setId(1000L);
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setWarehouse(warehouse);
        receipt.setVersion(1);
        receipt.setReceiptNumber("RET-001");

        Batch batch = new Batch();
        batch.setId(200L);

        ReceiptItem item = new ReceiptItem();
        item.setId(1001L);
        item.setProduct(product);
        item.setBatch(batch);
        item.setSamplePassedQty(4);
        item.setSampleFailedQty(1);

        WarehouseLocation passedLoc = new WarehouseLocation();
        passedLoc.setId(80L);
        passedLoc.setIsQuarantine(false); // regular location

        WarehouseLocation failedLoc = new WarehouseLocation();
        failedLoc.setId(81L);
        failedLoc.setIsQuarantine(true); // quarantine location

        ReturnPutawayRequest request = new ReturnPutawayRequest();
        request.setExpectedVersion(1);

        ReturnPutawayItemRequest putawayItem = new ReturnPutawayItemRequest();
        putawayItem.setReceiptItemId(1001L);
        putawayItem.setPassedLocationId(80L);
        putawayItem.setFailedLocationId(81L);
        request.setPutawayItems(List.of(putawayItem));

        Inventory passedInv = new Inventory();
        passedInv.setId(400L);
        passedInv.setTotalQty(new BigDecimal("10.0"));
        passedInv.setReservedQty(BigDecimal.ZERO);

        Inventory failedInv = new Inventory();
        failedInv.setId(401L);
        failedInv.setTotalQty(new BigDecimal("2.0"));
        failedInv.setReservedQty(BigDecimal.ZERO);

        when(receiptRepository.findById(1000L)).thenReturn(Optional.of(receipt));
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(20L)).thenReturn(List.of(1L));
        when(receiptItemRepository.findByReceiptId(1000L)).thenReturn(List.of(item));
        when(warehouseLocationRepository.findById(80L)).thenReturn(Optional.of(passedLoc));
        when(warehouseLocationRepository.findById(81L)).thenReturn(Optional.of(failedLoc));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 80L))
                .thenReturn(Optional.of(passedInv));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(1L, 100L, 200L, 81L))
                .thenReturn(Optional.of(failedInv));

        Receipt result = returnService.completePutaway(1000L, request, storekeeper);

        assertNotNull(result);
        assertEquals(new BigDecimal("14.0"), passedInv.getTotalQty()); // 10 + 4
        assertEquals(new BigDecimal("3.0"), failedInv.getTotalQty()); // 2 + 1
        assertEquals(passedLoc, item.getLocation());

        verify(inventoryRepository, times(1)).save(passedInv);
        verify(inventoryRepository, times(1)).save(failedInv);
        verify(receiptItemRepository, times(1)).save(item);
    }

    @Test
    void createCreditNote_happyPath_updatesDealerBalance() {
        Receipt receipt = new Receipt();
        receipt.setId(1000L);
        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setWarehouse(warehouse);
        receipt.setDealer(dealer);
        receipt.setDeliveryOrder(deliveryOrder);
        receipt.setVersion(1);
        receipt.setReceiptNumber("RET-001");

        ReceiptItem item = new ReceiptItem();
        item.setId(1001L);
        item.setProduct(product);
        item.setActualQty(4);

        ReturnCreditNoteRequest request = new ReturnCreditNoteRequest();
        request.setExpectedVersion(1);
        request.setReason("Returns refund");

        when(receiptRepository.findById(1000L)).thenReturn(Optional.of(receipt));
        when(creditNoteRepository.existsByReceiptId(1000L)).thenReturn(false);
        when(accountingPeriodRepository.findPeriodByDateAndStatus(any(), eq(AccountingPeriodStatus.OPEN)))
                .thenReturn(Optional.of(openPeriod));
        when(receiptItemRepository.findByReceiptId(1000L)).thenReturn(List.of(item));
        when(dealerRepository.save(dealer)).thenReturn(dealer);
        when(deliveryOrderItemRepository.findByDeliveryOrderId(300L)).thenReturn(List.of(doItem));

        ReturnCreditNoteResponse response = returnService.createCreditNote(1000L, request, accountant);

        // Refund amount = 4 actual * 1.5M = 6.0M VND
        assertNotNull(response);
        assertEquals(new BigDecimal("6000000.00"), response.getAmount());
        assertEquals(5L, response.getDealerId());

        // Dealer new balance: 50M - 6M = 44M VND
        assertEquals(new BigDecimal("44000000.00"), dealer.getCurrentBalance());

        verify(creditNoteRepository, times(1)).save(any(CreditNote.class));
        verify(dealerRepository, times(1)).save(dealer);
        verify(auditLogService, times(1)).log(eq(accountant), eq(AuditAction.CREDIT_NOTE_CREATE), eq("CREDIT_NOTE"), any(), any(), eq(1L), any(), any());
    }
}
