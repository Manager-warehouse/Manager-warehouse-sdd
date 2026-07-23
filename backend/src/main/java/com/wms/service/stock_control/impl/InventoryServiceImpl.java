package com.wms.service.stock_control.impl;


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
import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.StockAlertRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.stock_control.InventoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ReceiptRepository receiptRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final StockAlertRepository stockAlertRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                WarehouseRepository warehouseRepository,
                                ProductRepository productRepository,
                                ReceiptRepository receiptRepository,
                                DeliveryOrderRepository deliveryOrderRepository,
                                StockAlertRepository stockAlertRepository) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.receiptRepository = receiptRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.stockAlertRepository = stockAlertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAvailabilityResponse getAvailability(Long warehouseId, Long productId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new IllegalArgumentException("WAREHOUSE_NOT_FOUND");
        }
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("PRODUCT_NOT_FOUND");
        }
        InventoryRepository.AvailabilitySummary summary =
                inventoryRepository.summarizeAvailability(warehouseId, productId);
        return new InventoryAvailabilityResponse(
                warehouseId,
                productId,
                summary.getTotalQty(),
                summary.getReservedQty(),
                summary.getAvailableQty());
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseStockOverviewResponse getOverview(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new IllegalArgumentException("WAREHOUSE_NOT_FOUND");
        }

        LocalDate today = LocalDate.now();
        BigDecimal availableQty = inventoryRepository.sumValidAvailableQtyByWarehouse(warehouseId);
        return new WarehouseStockOverviewResponse(
                warehouseId,
                availableQty != null ? availableQty : BigDecimal.ZERO,
                receiptRepository.countByWarehouseIdAndDocumentDate(warehouseId, today),
                deliveryOrderRepository.countByWarehouseIdAndDocumentDate(warehouseId, today),
                stockAlertRepository.countByWarehouseIdAndIsResolvedFalse(warehouseId));
    }
}
