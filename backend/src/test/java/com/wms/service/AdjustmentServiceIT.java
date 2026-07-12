package com.wms.service;

import com.wms.dto.request.CreateStockTakeRequest;
import com.wms.dto.request.StockTakeCountItemRequest;
import com.wms.dto.request.StockTakeCountRequest;
import com.wms.dto.response.StockTakeResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:adjustmenttestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class AdjustmentServiceIT {

    @Autowired
    private StockTakeService stockTakeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository locationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AccountingPeriodRepository periodRepository;

    @Autowired
    private DocumentSequenceRepository sequenceRepository;

    @Autowired
    private StockTakeRepository stockTakeRepository;

    @Autowired
    private StockTakeItemRepository stockTakeItemRepository;

    @Autowired
    private AdjustmentRepository adjustmentRepository;

    @Autowired
    private UserWarehouseAssignmentRepository assignmentRepository;

    private User storekeeper;
    private User manager;
    private Warehouse warehouse;
    private WarehouseLocation binLoc;
    private Product product;
    private Batch batch;
    private Inventory inventory;
    private AccountingPeriod period;

    @BeforeEach
    void setUp() {
        adjustmentRepository.deleteAll();
        stockTakeItemRepository.deleteAll();
        stockTakeRepository.deleteAll();
        inventoryRepository.deleteAll();
        batchRepository.deleteAll();
        assignmentRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        periodRepository.deleteAll();
        userRepository.deleteAll();
        sequenceRepository.deleteAll();

        // 1. Setup Document Sequence for ST
        DocumentSequence seq = new DocumentSequence();
        seq.setSequenceKey("ST");
        seq.setNextValue(1L);
        seq.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(seq);

        // 2. Setup users
        storekeeper = userRepository.save(User.builder()
                .code("SK001").fullName("Storekeeper").email("store@wms.com")
                .passwordHash("hash").role(UserRole.STOREKEEPER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        manager = userRepository.save(User.builder()
                .code("MG001").fullName("Manager").email("manager@wms.com")
                .passwordHash("hash").role(UserRole.WAREHOUSE_MANAGER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 3. Setup Warehouse & Assignments
        warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MAIN").name("Main Warehouse").type(WarehouseType.PHYSICAL).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        for (User u : List.of(storekeeper, manager)) {
            UserWarehouseAssignment assign = new UserWarehouseAssignment();
            assign.setUser(u);
            assign.setWarehouse(warehouse);
            assign.setAssignedBy(storekeeper);
            assign.setAssignedAt(OffsetDateTime.now());
            assignmentRepository.save(assign);
        }

        // 4. Setup Period
        period = periodRepository.save(AccountingPeriod.builder()
                .periodName("JUN-2026").startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().plusDays(20))
                .status(AccountingPeriodStatus.OPEN).createdAt(OffsetDateTime.now()).build());

        // 5. Setup Locations
        binLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(warehouse).code("BIN-10").type(LocationType.BIN)
                .isQuarantine(false).isActive(true).isLocked(false)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 6. Setup Product & Batch
        product = productRepository.save(Product.builder()
                .sku("PROD-800").name("Xoong Inox").unit("PCS")
                .isActive(true).weightKg(new BigDecimal("1.00")).volumeM3(new BigDecimal("0.01"))
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        batch = batchRepository.save(Batch.builder()
                .batchNumber("BCH-800").product(product).warehouse(warehouse)
                .receivedDate(LocalDate.now().minusDays(5)).quantity(new BigDecimal("100.00"))
                .createdAt(OffsetDateTime.now()).build());

        // 7. Seed inventory
        inventory = inventoryRepository.save(Inventory.builder()
                .warehouse(warehouse).product(product).batch(batch).location(binLoc)
                .totalQty(new BigDecimal("100.00")).reservedQty(BigDecimal.ZERO)
                .costPrice(new BigDecimal("10000.00")).updatedAt(OffsetDateTime.now()).build());
    }

    @Test
    void testStockTakeDiscrepancy_autoApprovesAndUpdatesInventory() {
        // 1. Create StockTake (status = DRAFT)
        CreateStockTakeRequest createReq = new CreateStockTakeRequest();
        createReq.setWarehouseId(warehouse.getId());
        createReq.setStockTakeDate(LocalDate.now());
        createReq.setDocumentDate(LocalDate.now());
        createReq.setAccountingPeriodId(period.getId());

        StockTakeResponse stResp = stockTakeService.createStockTake(createReq, storekeeper);
        assertThat(stResp.getStatus()).isEqualTo(StockTakeStatus.DRAFT);
        assertThat(stResp.getItems()).hasSize(1);
        Long itemId = stResp.getItems().get(0).getId();

        // 2. Start StockTake (status becomes IN_PROGRESS, bin location is locked)
        stResp = stockTakeService.startStockTake(stResp.getId(), storekeeper);
        assertThat(stResp.getStatus()).isEqualTo(StockTakeStatus.IN_PROGRESS);

        WarehouseLocation lockedBin = locationRepository.findById(binLoc.getId()).orElseThrow();
        assertThat(lockedBin.getIsLocked()).isTrue();
        assertThat(lockedBin.getLockedByStockTakeId()).isEqualTo(stResp.getId());

        // 3. Record Count (actual quantity = 90, meaning variance = -10, variance value = -100,000 VND)
        StockTakeCountRequest countReq = new StockTakeCountRequest();
        StockTakeCountItemRequest countItem = new StockTakeCountItemRequest();
        countItem.setItemId(itemId);
        countItem.setActualQty(new BigDecimal("90.00"));
        countItem.setIsEmployeeFault(false);
        countItem.setNotes("Thiếu 10 cái xoong");
        countReq.setItems(List.of(countItem));

        stockTakeService.recordCount(stResp.getId(), countReq, storekeeper);

        // 4. Complete StockTake
        // Total variance value is -100,000 VND (magnitude < 5,000,000 VND threshold for auto-approval)
        // Therefore, completing this StockTake should trigger AUTO approval!
        stResp = stockTakeService.completeStockTake(stResp.getId(), storekeeper);
        assertThat(stResp.getStatus()).isEqualTo(StockTakeStatus.APPROVED);
        assertThat(stResp.getApprovalLevel()).isEqualTo(ApprovalLevel.AUTO);

        // Verify Location is unlocked
        WarehouseLocation unlockedBin = locationRepository.findById(binLoc.getId()).orElseThrow();
        assertThat(unlockedBin.getIsLocked()).isFalse();
        assertThat(unlockedBin.getLockedByStockTakeId()).isNull();

        // Verify Inventory is updated to 90.00
        Inventory updatedInventory = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(updatedInventory.getTotalQty()).isEqualByComparingTo(new BigDecimal("90.00"));

        // Verify Adjustment record created in database
        List<Adjustment> adjustments = adjustmentRepository.findAll();
        assertThat(adjustments).isNotEmpty();
        Adjustment adj = adjustments.stream()
                .filter(a -> a.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Adjustment not created"));

        assertThat(adj.getType()).isEqualTo(AdjustmentType.STOCK_TAKE);
        assertThat(adj.getQuantityAdjustment()).isEqualByComparingTo(new BigDecimal("-10.00"));
    }
}
