package com.wms.service.impl;

import com.wms.entity.Batch;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.ReceiptItem;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AdjustmentType;
import com.wms.enums.BatchGrade;
import com.wms.enums.QcResult;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.BatchRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.WarehouseLocationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReceiptInventoryHelper {

    private final InventoryRepository inventoryRepository;
    private final BatchRepository batchRepository;
    private final WarehouseLocationRepository locationRepository;

    public ReceiptInventoryHelper(InventoryRepository inventoryRepository,
                                  BatchRepository batchRepository,
                                  WarehouseLocationRepository locationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.batchRepository = batchRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public Batch createOrUpdateBatch(String batchNumber, Product product,
                                     Warehouse warehouse, LocalDate receivedDate,
                                     BatchGrade grade, BigDecimal qty) {
        return batchRepository.findByBatchNumber(batchNumber).map(b -> {
            b.setQuantity(b.getQuantity().add(qty));
            return batchRepository.save(b);
        }).orElseGet(() -> batchRepository.save(Batch.builder()
                .batchNumber(batchNumber)
                .product(product)
                .warehouse(warehouse)
                .receivedDate(receivedDate)
                .grade(grade)
                .quantity(qty)
                .createdAt(OffsetDateTime.now())
                .build()));
    }

    @Transactional
    public void upsertInventory(Warehouse warehouse, Product product,
                                Batch batch, WarehouseLocation location,
                                BigDecimal qty, BigDecimal costPrice) {
        inventoryRepository.findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
                warehouse.getId(), product.getId(), batch.getId(), location.getId()
        ).ifPresentOrElse(inv -> {
            inv.setTotalQty(inv.getTotalQty().add(qty));
            inv.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inv);
        }, () -> inventoryRepository.save(Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .totalQty(qty)
                .reservedQty(BigDecimal.ZERO)
                .costPrice(costPrice != null ? costPrice : BigDecimal.ZERO)
                .version(0)
                .updatedAt(OffsetDateTime.now())
                .build()));
    }

    @Transactional
    public void upsertQuarantineInventory(Warehouse warehouse, Product product,
                                          WarehouseLocation location, BigDecimal qty,
                                          BigDecimal costPrice) {
        inventoryRepository.findQuarantineInventory(
                warehouse.getId(), product.getId(), location.getId()
        ).ifPresentOrElse(inv -> {
            inv.setTotalQty(inv.getTotalQty().add(qty));
            inv.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inv);
        }, () -> inventoryRepository.save(Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(null)
                .location(location)
                .totalQty(qty)
                .reservedQty(BigDecimal.ZERO)
                .costPrice(costPrice != null ? costPrice : BigDecimal.ZERO)
                .version(0)
                .updatedAt(OffsetDateTime.now())
                .build()));
    }

    public WarehouseLocation requireQuarantineLocation(Long warehouseId) {
        return locationRepository.findFirstByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(warehouseId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No active quarantine location found for warehouse: " + warehouseId));
    }

    public WarehouseLocation requireLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
    }

    public void deductQuarantineInventory(Warehouse warehouse, Product product,
                                          WarehouseLocation location, BigDecimal qty) {
        Inventory inv = inventoryRepository.findQuarantineInventory(
                        warehouse.getId(), product.getId(), location.getId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Quarantine inventory not found for product: " + product.getSku()));
        if (inv.getTotalQty().compareTo(qty) < 0) {
            throw new BusinessRuleViolationException(
                    "Quarantine qty insufficient for product: " + product.getSku());
        }
        inv.setTotalQty(inv.getTotalQty().subtract(qty));
        inv.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inv);
    }

    public QcResult computeItemQcResult(BigDecimal sampleQty, BigDecimal passedQty, BigDecimal failedQty) {
        if (failedQty.compareTo(BigDecimal.ZERO) == 0) return QcResult.PASSED;
        if (passedQty.compareTo(BigDecimal.ZERO) == 0) return QcResult.FAILED;
        return QcResult.PARTIAL;
    }
}
