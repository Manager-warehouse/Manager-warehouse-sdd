package com.wms.service;

import com.wms.entity.Batch;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.LocationType;
import com.wms.enums.WarehouseType;
import com.wms.repository.BatchRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:lockingtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.mail.host=localhost",
    "spring.mail.port=25",
    "jwt.secret=9a4f2c8d3b7a1e5f8c2d6e0b4a8f9c1d3e7b2a6f0c4d8e2f6a0b4c8d2e6f0a4b",
    "jwt.access-token-expiry=900",
    "jwt.refresh-token-expiry=604800"
})
public class InventoryOptimisticLockingIT {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private WarehouseLocationRepository locationRepository;

    private Long inventoryId;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        batchRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        warehouseRepository.deleteAll();

        // 1. Save dependencies
        Warehouse warehouse = Warehouse.builder()
                .code("WH001")
                .name("Test Warehouse")
                .type(WarehouseType.PHYSICAL)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        warehouse = warehouseRepository.save(warehouse);

        Product product = Product.builder()
                .sku("SKU001")
                .name("Test Product")
                .unit("PCS")
                .isActive(true)
                .build();
        product = productRepository.save(product);

        Batch batch = Batch.builder()
                .batchNumber("B001")
                .product(product)
                .warehouse(warehouse)
                .receivedDate(LocalDate.now())
                .quantity(BigDecimal.TEN)
                .createdAt(OffsetDateTime.now())
                .build();
        batch = batchRepository.save(batch);

        WarehouseLocation location = WarehouseLocation.builder()
                .warehouse(warehouse)
                .code("LOC001")
                .type(LocationType.BIN)
                .currentVolumeM3(BigDecimal.ZERO)
                .currentWeightKg(BigDecimal.ZERO)
                .isQuarantine(false)
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        location = locationRepository.save(location);

        // 2. Save inventory record
        Inventory inventory = Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .batch(batch)
                .location(location)
                .totalQty(BigDecimal.TEN)
                .reservedQty(BigDecimal.ZERO)
                .costPrice(BigDecimal.ONE)
                .updatedAt(OffsetDateTime.now())
                .build();
        inventory = inventoryRepository.save(inventory);
        inventoryId = inventory.getId();
    }

    @Test
    void optimisticLocking_concurrentUpdates_throwsException() {
        // Read inventory from DB in session A
        Inventory sessionA = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AssertionError("Inventory not found"));

        // Read same inventory from DB in session B
        Inventory sessionB = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AssertionError("Inventory not found"));

        assertEquals(0, sessionA.getVersion());
        assertEquals(0, sessionB.getVersion());

        // Update total quantity in session A and save
        sessionA.setTotalQty(new BigDecimal("15.00"));
        sessionA.setUpdatedAt(OffsetDateTime.now());
        sessionA = inventoryRepository.saveAndFlush(sessionA);

        // Assert version incremented
        assertEquals(1, sessionA.getVersion());

        // Try to update in session B using stale version 0 and save
        sessionB.setTotalQty(new BigDecimal("20.00"));
        sessionB.setUpdatedAt(OffsetDateTime.now());

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            inventoryRepository.saveAndFlush(sessionB);
        });
    }
}
