package com.wms.service.impl;

import com.wms.dto.response.InventoryAvailabilityResponse;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                WarehouseRepository warehouseRepository,
                                ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
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
}
