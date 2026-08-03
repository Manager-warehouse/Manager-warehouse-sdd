package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.response.QuarantineItemResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.stock_receiving.QuarantineRecord;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DebitNoteRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.stock_receiving.QuarantineRecordRepository;
import com.wms.repository.stock_receiving.ReceiptItemRepository;
import com.wms.repository.stock_receiving.ReceiptRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.stock_receiving.QuarantineRtvService;
import com.wms.service.stock_receiving.ReceiptValidationService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuarantineRtvServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private DebitNoteRepository debitNoteRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ReceiptValidationService receiptValidationService;
    @Mock private AuditLogService auditLogService;
    @Mock private QuarantineRecordRepository quarantineRecordRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private AccountingPeriodService accountingPeriodService;

    @InjectMocks private QuarantineRtvService service;

    @Test
    void getQuarantineItems_includesOutboundQcRemainingQuantityWithoutTruncation() {
        User actor = new User();
        actor.setId(10L);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        Product product = new Product();
        product.setId(20L);
        product.setSku("SKU-20");
        product.setName("Nồi inox");
        product.setUnit("cái");
        Batch batch = new Batch();
        batch.setId(30L);
        WarehouseLocation location = new WarehouseLocation();
        location.setId(40L);
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(50L);
        deliveryOrder.setDoNumber("DO-50");
        Dealer dealer = new Dealer();
        dealer.setId(70L);
        dealer.setName("Đại lý A");
        deliveryOrder.setDealer(dealer);

        QuarantineRecord record = new QuarantineRecord();
        record.setId(60L);
        record.setWarehouse(warehouse);
        record.setProduct(product);
        record.setBatch(batch);
        record.setLocation(location);
        record.setDeliveryOrder(deliveryOrder);
        record.setOriginType("OUTBOUND_QC");
        record.setQuantity(new BigDecimal("0.50"));
        record.setRemainingQuantity(new BigDecimal("0.50"));
        record.setReason("Trầy xước");
        record.setCreatedAt(OffsetDateTime.now());

        Inventory inventory = new Inventory();
        inventory.setCostPrice(new BigDecimal("100000.00"));
        when(receiptItemRepository.findQuarantineItemsByWarehouseId(1L)).thenReturn(List.of());
        when(quarantineRecordRepository
                .findByWarehouseIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(1L, BigDecimal.ZERO))
                .thenReturn(List.of(record));
        when(inventoryRepository.findByWarehouseProductBatchLocation(1L, 20L, 30L, 40L))
                .thenReturn(Optional.of(inventory));

        List<QuarantineItemResponse> result = service.getQuarantineItems(1L, actor);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getQuarantineRecordId()).isEqualTo(60L);
            assertThat(item.getOriginType()).isEqualTo("OUTBOUND_QC");
            assertThat(item.getQcFailedQty()).isEqualByComparingTo("0.50");
            assertThat(item.getTotalValue()).isEqualByComparingTo("50000.0000");
            assertThat(item.getReceiptNumber()).isEqualTo("DO-50");
            assertThat(item.getDealerId()).isEqualTo(70L);
            assertThat(item.getDealerName()).isEqualTo("Đại lý A");
        });
        verify(receiptValidationService).assertWarehouseAccess(actor, 1L);
    }
}
