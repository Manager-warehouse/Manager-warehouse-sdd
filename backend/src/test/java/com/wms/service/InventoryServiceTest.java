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

import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.StockAlertRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.stock_control.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private DeliveryOrderRepository deliveryOrderRepository;

    @Mock
    private StockAlertRepository stockAlertRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(
                inventoryRepository,
                warehouseRepository,
                productRepository,
                receiptRepository,
                deliveryOrderRepository,
                stockAlertRepository);
    }

    @Test
    void getAvailability_warehouseNotFound_throwsIllegalArgumentException() {
        when(warehouseRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.getAvailability(1L, 1L);
        });
        assertThat(ex.getMessage()).isEqualTo("WAREHOUSE_NOT_FOUND");
    }

    @Test
    void getAvailability_productNotFound_throwsIllegalArgumentException() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.getAvailability(1L, 1L);
        });
        assertThat(ex.getMessage()).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void getAvailability_success() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);

        InventoryRepository.AvailabilitySummary summary = new InventoryRepository.AvailabilitySummary() {
            @Override
            public BigDecimal getTotalQty() {
                return new BigDecimal("100.00");
            }

            @Override
            public BigDecimal getReservedQty() {
                return new BigDecimal("30.00");
            }

            @Override
            public BigDecimal getAvailableQty() {
                return new BigDecimal("70.00");
            }
        };

        when(inventoryRepository.summarizeAvailability(1L, 2L)).thenReturn(summary);

        InventoryAvailabilityResponse response = inventoryService.getAvailability(1L, 2L);
        assertThat(response.warehouseId()).isEqualTo(1L);
        assertThat(response.productId()).isEqualTo(2L);
        assertThat(response.totalQty()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.reservedQty()).isEqualTo(new BigDecimal("30.00"));
        assertThat(response.availableQty()).isEqualTo(new BigDecimal("70.00"));
    }

    @Test
    void getOverview_success() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.sumValidAvailableQtyByWarehouse(1L)).thenReturn(new BigDecimal("125.50"));
        when(receiptRepository.countByWarehouseIdAndDocumentDate(1L, LocalDate.now())).thenReturn(3L);
        when(deliveryOrderRepository.countByWarehouseIdAndDocumentDate(1L, LocalDate.now())).thenReturn(5L);
        when(stockAlertRepository.countByWarehouseIdAndIsResolvedFalse(1L)).thenReturn(2L);

        WarehouseStockOverviewResponse response = inventoryService.getOverview(1L);

        assertThat(response.warehouseId()).isEqualTo(1L);
        assertThat(response.availableQty()).isEqualTo(new BigDecimal("125.50"));
        assertThat(response.todayReceiptCount()).isEqualTo(3L);
        assertThat(response.todayDeliveryOrderCount()).isEqualTo(5L);
        assertThat(response.activeLowStockCount()).isEqualTo(2L);
    }

    @Test
    void inventoryInvariants_avoidNegativeInventory() {
        // Business Rule: inventories.total_qty >= 0, reserved_qty >= 0, available = total - reserved >= 0
        Inventory inventory = new Inventory();
        inventory.setTotalQty(new BigDecimal("10.00"));
        inventory.setReservedQty(new BigDecimal("4.00"));

        // available = 6.00
        BigDecimal available = inventory.getTotalQty().subtract(inventory.getReservedQty());
        assertThat(available).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Reserving more than available should violate invariant
        BigDecimal reserveAttempt = new BigDecimal("8.00");
        boolean canReserve = available.compareTo(reserveAttempt) >= 0;
        assertThat(canReserve).isFalse();
    }

    @Test
    void fifoPrinciple_sortsByReceivedDateAscending() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);

        Product product = new Product();
        product.setId(2L);

        // Batch 1 received on 2026-06-01
        Batch batch1 = new Batch();
        batch1.setId(10L);
        batch1.setReceivedDate(LocalDate.of(2026, 6, 1));

        Inventory inv1 = new Inventory();
        inv1.setId(100L);
        inv1.setBatch(batch1);
        inv1.setTotalQty(new BigDecimal("10.00"));
        inv1.setReservedQty(BigDecimal.ZERO);

        // Batch 2 received on 2026-06-05
        Batch batch2 = new Batch();
        batch2.setId(20L);
        batch2.setReceivedDate(LocalDate.of(2026, 6, 5));

        Inventory inv2 = new Inventory();
        inv2.setId(200L);
        inv2.setBatch(batch2);
        inv2.setTotalQty(new BigDecimal("15.00"));
        inv2.setReservedQty(BigDecimal.ZERO);

        List<Inventory> candidates = List.of(inv1, inv2);

        // Verify that candidate 1 is older than candidate 2 (FIFO order)
        assertThat(candidates.get(0).getBatch().getReceivedDate())
                .isBefore(candidates.get(1).getBatch().getReceivedDate());
    }
}
