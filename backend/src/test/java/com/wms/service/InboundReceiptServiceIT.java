package com.wms.service;

import com.wms.dto.request.CreateReceiptItemRequest;
import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.ReceiveReceiptItemRequest;
import com.wms.dto.request.ReceiveReceiptRequest;
import com.wms.dto.request.ReceiptQcItemRequest;
import com.wms.dto.request.ReceiptQcRequest;
import com.wms.dto.response.ReceiptQcResponse;
import com.wms.dto.response.ReceiptResponse;
import com.wms.entity.DocumentSequence;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.Supplier;
import com.wms.entity.User;
import com.wms.entity.UserWarehouseAssignment;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.LocationType;
import com.wms.enums.ReceiptSourceChannel;
import com.wms.enums.ReceiptStatus;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.enums.QcSamplingMethod;
import com.wms.enums.QcResult;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ProductRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.SupplierRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:inboundtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class InboundReceiptServiceIT {

    @Autowired
    private ReceiptService receiptService;

    @Autowired
    private ReceiptQcService receiptQcService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository locationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DocumentSequenceRepository sequenceRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private ReceiptItemRepository receiptItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserWarehouseAssignmentRepository assignmentRepository;

    private User planner;
    private User staff;
    private User storekeeper;
    private Supplier supplier;
    private Warehouse warehouse;
    private WarehouseLocation quarantineLoc;
    private WarehouseLocation regularLoc;
    private Product product;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        receiptItemRepository.deleteAll();
        receiptRepository.deleteAll();
        assignmentRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        supplierRepository.deleteAll();
        userRepository.deleteAll();
        sequenceRepository.deleteAll();

        // Initialize Receipt Sequence
        DocumentSequence seq = new DocumentSequence();
        seq.setSequenceKey("RECEIPT");
        seq.setNextValue(1L);
        seq.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(seq);

        // Save users
        planner = userRepository.save(User.builder()
                .code("PL001").fullName("Planner").email("plan@wms.com")
                .passwordHash("hash").role(UserRole.PLANNER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        staff = userRepository.save(User.builder()
                .code("ST001").fullName("Staff").email("staff@wms.com")
                .passwordHash("hash").role(UserRole.WAREHOUSE_STAFF).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        storekeeper = userRepository.save(User.builder()
                .code("SK002").fullName("Storekeeper").email("store@wms.com")
                .passwordHash("hash").role(UserRole.STOREKEEPER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // Save supplier & warehouse
        supplier = supplierRepository.save(Supplier.builder()
                .code("SUP001").companyName("Supplier 1").isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MAIN").name("Main Warehouse").type(WarehouseType.PHYSICAL).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // Save assignments
        UserWarehouseAssignment assign1 = new UserWarehouseAssignment();
        assign1.setUser(planner);
        assign1.setWarehouse(warehouse);
        assign1.setAssignedBy(planner);
        assign1.setAssignedAt(OffsetDateTime.now());
        assignmentRepository.save(assign1);

        UserWarehouseAssignment assign2 = new UserWarehouseAssignment();
        assign2.setUser(staff);
        assign2.setWarehouse(warehouse);
        assign2.setAssignedBy(planner);
        assign2.setAssignedAt(OffsetDateTime.now());
        assignmentRepository.save(assign2);

        UserWarehouseAssignment assign3 = new UserWarehouseAssignment();
        assign3.setUser(storekeeper);
        assign3.setWarehouse(warehouse);
        assign3.setAssignedBy(planner);
        assign3.setAssignedAt(OffsetDateTime.now());
        assignmentRepository.save(assign3);

        // Save locations
        regularLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(warehouse).code("LOC-REGULAR").type(LocationType.BIN)
                .isQuarantine(false).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        quarantineLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(warehouse).code("LOC-QUARANTINE").type(LocationType.BIN)
                .isQuarantine(true).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // Save product
        product = productRepository.save(Product.builder()
                .sku("PROD-001").name("Nồi Inox").unit("PCS")
                .isActive(true).weightKg(new BigDecimal("1.50")).volumeM3(new BigDecimal("0.02"))
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());
    }

    @Test
    void testInboundReceipt_qcFailed_movesToQuarantine() {
        // 1. Create Purchase Receipt Draft (status = PENDING_RECEIPT)
        CreateReceiptRequest createReq = new CreateReceiptRequest();
        createReq.setSupplierId(supplier.getId());
        createReq.setWarehouseId(warehouse.getId());
        createReq.setSourceReference("PO-10001");
        createReq.setContactPerson("N/A");
        createReq.setSourceChannel(ReceiptSourceChannel.EMAIL);
        createReq.setNotes("Test receipt");

        CreateReceiptItemRequest itemReq = new CreateReceiptItemRequest();
        itemReq.setProductId(product.getId());
        itemReq.setExpectedQty(10);
        itemReq.setUnitCost(new BigDecimal("150.00"));
        createReq.setItems(List.of(itemReq));

        ReceiptResponse receiptResp = receiptService.createPurchaseReceipt(createReq, planner);
        assertThat(receiptResp.getStatus()).isEqualTo(ReceiptStatus.PENDING_RECEIPT.name());
        assertThat(receiptResp.getItems()).hasSize(1);
        Long receiptItemId = receiptResp.getItems().get(0).getReceiptItemId();

        // 2. Submit Physical Counts (receiveReceiptCounts)
        ReceiveReceiptRequest receiveReq = new ReceiveReceiptRequest();
        ReceiveReceiptItemRequest countReq = new ReceiveReceiptItemRequest();
        countReq.setReceiptItemId(receiptItemId);
        countReq.setCountedQty(10);
        receiveReq.setItems(List.of(countReq));

        receiptResp = receiptService.receiveReceiptCounts(receiptResp.getId(), receiveReq, staff);
        assertThat(receiptResp.getStatus()).isEqualTo(ReceiptStatus.DRAFT.name());
        assertThat(receiptResp.getItems().get(0).getActualQty()).isEqualTo(10);

        // 3. Submit QC Results (SUBMIT action)
        ReceiptQcRequest qcSubmitReq = new ReceiptQcRequest();
        qcSubmitReq.setAction(ReceiptQcRequest.QcAction.SUBMIT);
        ReceiptQcItemRequest qcItemReq = new ReceiptQcItemRequest();
        qcItemReq.setReceiptItemId(receiptItemId);
        qcItemReq.setSampleQty(10);
        qcItemReq.setQcPassedQty(6);
        qcItemReq.setQcFailedQty(4);
        qcItemReq.setQcSamplingMethod(QcSamplingMethod.FULL_INSPECTION);
        qcItemReq.setQcFailureReason("Móp méo");
        qcSubmitReq.setItems(List.of(qcItemReq));

        ReceiptQcResponse qcResp = receiptQcService.processQc(receiptResp.getId(), qcSubmitReq, staff.getEmail());
        assertThat(qcResp.getItems().get(0).getQcResult()).isEqualTo(QcResult.FAILED);

        // 4. Confirm QC (CONFIRM action) -> Status becomes QC_FAILED or QC_COMPLETED
        ReceiptQcRequest qcConfirmReq = new ReceiptQcRequest();
        qcConfirmReq.setAction(ReceiptQcRequest.QcAction.CONFIRM);
        qcResp = receiptQcService.processQc(receiptResp.getId(), qcConfirmReq, storekeeper.getEmail());
        assertThat(qcResp.getStatus()).isEqualTo(ReceiptStatus.QC_FAILED);

        // Verify that failed items (4) are moved to Quarantine Area
        List<Inventory> inventoryList = inventoryRepository.findAll();
        assertThat(inventoryList).isNotEmpty();

        // Should find quarantine inventory with totalQty = 4
        Inventory quarantineInventory = inventoryList.stream()
                .filter(inv -> inv.getLocation().getId().equals(quarantineLoc.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No quarantine inventory found"));

        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(quarantineInventory.getReservedQty()).isEqualByComparingTo(BigDecimal.ZERO);

        // Verify Quarantine Location capacity updates
        WarehouseLocation updatedQuarantine = locationRepository.findById(quarantineLoc.getId()).orElseThrow();
        // 4 * 1.50 = 6.00 kg
        assertThat(updatedQuarantine.getCurrentWeightKg()).isEqualByComparingTo(new BigDecimal("6.00"));
        // 4 * 0.02 = 0.08 m3
        assertThat(updatedQuarantine.getCurrentVolumeM3()).isEqualByComparingTo(new BigDecimal("0.08"));
    }
}
