package com.wms.service;

import com.wms.dto.request.*;
import com.wms.dto.response.CreditNoteResponse;
import com.wms.dto.response.ReceiptActionResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnsServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private ReceiptValidationService receiptValidationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ReturnsService returnsService;

    private User actor;
    private Dealer dealer;
    private Warehouse warehouse;
    private DeliveryOrder deliveryOrder;
    private DeliveryOrderItem doItem;
    private Product product;
    private Batch batch;

    @BeforeEach
    void setUp() {
        actor = User.builder().id(1L).role(UserRole.WAREHOUSE_STAFF).fullName("Staff Test").build();
        dealer = Dealer.builder().id(5L).name("Dealer A").currentBalance(BigDecimal.valueOf(10000000)).build();
        warehouse = Warehouse.builder().id(10L).build();
        product = Product.builder().id(30L).sku("POT-001").volumeM3(BigDecimal.valueOf(0.1)).weightKg(BigDecimal.valueOf(1.0)).build();
        batch = Batch.builder().id(40L).build();

        deliveryOrder = DeliveryOrder.builder()
                .id(50L)
                .doNumber("DO-001")
                .dealer(dealer)
                .warehouse(warehouse)
                .status(DeliveryOrderStatus.DELIVERED)
                .build();

        doItem = DeliveryOrderItem.builder()
                .id(60L)
                .deliveryOrder(deliveryOrder)
                .product(product)
                .batch(batch)
                .issuedQty(BigDecimal.valueOf(20))
                .unitPrice(BigDecimal.valueOf(150000)) // Selling price 150K
                .build();
    }

    @Test
    void createReturnReceipt_success() {
        when(deliveryOrderRepository.findById(50L)).thenReturn(Optional.of(deliveryOrder));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(50L)).thenReturn(List.of(doItem));
        when(receiptRepository.findByDeliveryOrderIdAndType(50L, ReceiptType.RETURN)).thenReturn(Collections.emptyList());
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        CreateReturnRequest req = CreateReturnRequest.builder()
                .warehouseId(10L)
                .dealerId(5L)
                .deliveryOrderId(50L)
                .items(List.of(CreateReturnItemRequest.builder().productId(30L).expectedQty(5).build()))
                .notes("Trả chảo chống dính")
                .build();

        ReceiptActionResponse response = returnsService.createReturnReceipt(req, actor);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReceiptStatus.DRAFT);
        verify(receiptRepository).save(any(Receipt.class));
        verify(receiptItemRepository).save(any(ReceiptItem.class));
    }

    @Test
    void createReturnReceipt_exceedsOriginalSale_fails() {
        when(deliveryOrderRepository.findById(50L)).thenReturn(Optional.of(deliveryOrder));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(50L)).thenReturn(List.of(doItem));

        CreateReturnRequest req = CreateReturnRequest.builder()
                .warehouseId(10L)
                .dealerId(5L)
                .deliveryOrderId(50L)
                .items(List.of(CreateReturnItemRequest.builder().productId(30L).expectedQty(25).build())) // 25 > 20 issued
                .build();

        assertThatThrownBy(() -> returnsService.createReturnReceipt(req, actor))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("RETURN_EXCEEDS_ORIGINAL_SALE");
    }

    @Test
    void processReturnQc_success() {
        Receipt returnReceipt = Receipt.builder()
                .id(100L)
                .receiptNumber("REC-RET-001")
                .type(ReceiptType.RETURN)
                .warehouse(warehouse)
                .deliveryOrder(deliveryOrder)
                .status(ReceiptStatus.DRAFT)
                .version(1)
                .build();

        ReceiptItem receiptItem = ReceiptItem.builder()
                .id(200L)
                .receipt(returnReceipt)
                .product(product)
                .batch(batch)
                .expectedQty(10)
                .unitCost(BigDecimal.valueOf(150000))
                .build();

        WarehouseLocation regularBin = WarehouseLocation.builder().id(301L).isQuarantine(false).build();
        WarehouseLocation quarantineBin = WarehouseLocation.builder().id(302L).isQuarantine(true).build();

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(returnReceipt));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(any())).thenReturn(List.of(doItem));
        when(receiptItemRepository.findById(200L)).thenReturn(Optional.of(receiptItem));
        when(warehouseLocationRepository.findById(301L)).thenReturn(Optional.of(regularBin));
        when(warehouseLocationRepository.findById(302L)).thenReturn(Optional.of(quarantineBin));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReturnQcRequest qcReq = ReturnQcRequest.builder()
                .expectedVersion(1)
                .items(List.of(ReturnQcItemRequest.builder()
                        .receiptItemId(200L)
                        .actualQty(10)
                        .passedQty(8)
                        .failedQty(2)
                        .passedLocationId(301L)
                        .quarantineLocationId(302L)
                        .build()))
                .build();

        ReceiptActionResponse response = returnsService.processReturnQc(100L, qcReq, actor);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReceiptStatus.APPROVED);
        assertThat(receiptItem.getQcResult()).isEqualTo(QcResult.FAILED); // because failedQty = 2 > 0

        verify(inventoryRepository, times(2)).save(any(Inventory.class)); // 1 for regular, 1 for quarantine
    }

    @Test
    void createCreditNote_success() {
        Receipt approvedReceipt = Receipt.builder()
                .id(100L)
                .receiptNumber("REC-RET-001")
                .type(ReceiptType.RETURN)
                .warehouse(warehouse)
                .dealer(dealer)
                .status(ReceiptStatus.APPROVED)
                .build();

        ReceiptItem item1 = ReceiptItem.builder()
                .actualQty(10)
                .unitCost(BigDecimal.valueOf(150000)) // refund amount = 10 * 150K = 1.5M
                .build();

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(approvedReceipt));
        when(creditNoteRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(List.of(item1));
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(invocation -> {
            CreditNote cn = invocation.getArgument(0);
            cn.setId(600L);
            return cn;
        });

        CreateCreditNoteRequest req = CreateCreditNoteRequest.builder().reason("Trả hàng lỗi").build();
        User accountant = User.builder().id(4L).role(UserRole.ACCOUNTANT).build();

        CreditNoteResponse response = returnsService.createCreditNote(100L, req, accountant);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500000));
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(8500000)); // 10M - 1.5M = 8.5M
        
        verify(dealerRepository).save(dealer);
        verify(creditNoteRepository).save(any(CreditNote.class));
    }
}
