package com.wms.service.impl;

import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.dto.response.WarehouseStockOverviewResponse;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.StockAlertRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.InventoryService;
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
