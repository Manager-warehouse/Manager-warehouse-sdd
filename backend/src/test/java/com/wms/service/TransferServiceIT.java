package com.wms.service;

import com.wms.dto.request.*;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.repository.*;
import com.wms.service.transfer.InterWarehouseTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:transfertestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class TransferServiceIT {

    @Autowired
    private InterWarehouseTransferService transferService;

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
    private UserWarehouseAssignmentRepository assignmentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    private User planner;
    private User storekeeper;
    private User manager;
    private User driverUser;
    private Warehouse srcWarehouse;
    private Warehouse destWarehouse;
    private Warehouse transitWarehouse;
    private WarehouseLocation srcLoc;
    private WarehouseLocation destLoc;
    private WarehouseLocation transitLoc;
    private Product product;
    private Batch batch;
    private Inventory srcInventory;
    private Vehicle vehicle;
    private Driver driver;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        batchRepository.deleteAll();
        assignmentRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        tripRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        warehouseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Users
        planner = userRepository.save(User.builder()
                .code("PL001").fullName("Planner").email("plan@wms.com")
                .passwordHash("hash").role(UserRole.PLANNER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        storekeeper = userRepository.save(User.builder()
                .code("SK001").fullName("Storekeeper").email("store@wms.com")
                .passwordHash("hash").role(UserRole.STOREKEEPER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        manager = userRepository.save(User.builder()
                .code("MG001").fullName("Manager").email("manager@wms.com")
                .passwordHash("hash").role(UserRole.WAREHOUSE_MANAGER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        driverUser = userRepository.save(User.builder()
                .code("DR001").fullName("Driver User").email("driver@wms.com")
                .passwordHash("hash").role(UserRole.DRIVER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 2. Warehouses
        srcWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-SRC").name("Source Warehouse").type(WarehouseType.PHYSICAL).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        destWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-DEST").name("Destination Warehouse").type(WarehouseType.PHYSICAL).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        transitWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("IN_TRANSIT").name("Virtual Transit").type(WarehouseType.IN_TRANSIT).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // Save assignments
        for (User u : List.of(planner, storekeeper, manager, driverUser)) {
            UserWarehouseAssignment assignSrc = new UserWarehouseAssignment();
            assignSrc.setUser(u);
            assignSrc.setWarehouse(srcWarehouse);
            assignSrc.setAssignedBy(planner);
            assignSrc.setAssignedAt(OffsetDateTime.now());
            assignmentRepository.save(assignSrc);

            UserWarehouseAssignment assignDest = new UserWarehouseAssignment();
            assignDest.setUser(u);
            assignDest.setWarehouse(destWarehouse);
            assignDest.setAssignedBy(planner);
            assignDest.setAssignedAt(OffsetDateTime.now());
            assignmentRepository.save(assignDest);
        }

        // 3. Locations
        srcLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(srcWarehouse).code("SRC-BIN").type(LocationType.BIN)
                .isQuarantine(false).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        destLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(destWarehouse).code("DEST-BIN").type(LocationType.BIN)
                .isQuarantine(false).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        transitLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(transitWarehouse).code("TRANSIT-BIN").type(LocationType.BIN)
                .isQuarantine(false).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 4. Product
        product = productRepository.save(Product.builder()
                .sku("PROD-500").name("Chảo Gang").unit("PCS")
                .isActive(true).weightKg(new BigDecimal("2.00")).volumeM3(new BigDecimal("0.02"))
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 5. Batch & Inventory
        batch = batchRepository.save(Batch.builder()
                .batchNumber("BCH-500").product(product).warehouse(srcWarehouse)
                .receivedDate(LocalDate.now().minusDays(5)).quantity(new BigDecimal("100.00"))
                .createdAt(OffsetDateTime.now()).build());

        srcInventory = inventoryRepository.save(Inventory.builder()
                .warehouse(srcWarehouse).product(product).batch(batch).location(srcLoc)
                .totalQty(new BigDecimal("100.00")).reservedQty(BigDecimal.ZERO)
                .costPrice(new BigDecimal("120.00")).updatedAt(OffsetDateTime.now()).build());

        // 6. Logistics
        vehicle = vehicleRepository.save(Vehicle.builder()
                .plateNumber("29C-99999").vehicleType("TRUCK").maxWeightKg(new BigDecimal("5000.00"))
                .maxVolumeM3(new BigDecimal("20.00")).isActive(true).warehouse(srcWarehouse)
                .status(VehicleStatus.AVAILABLE)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        driver = driverRepository.save(Driver.builder()
                .user(driverUser).licenseNumber("B2-123456").status(DriverStatus.AVAILABLE)
                .fullName("Driver User").licenseExpiry(LocalDate.now().plusYears(5))
                .warehouse(srcWarehouse).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void testTransferLifecycle_happyPath() {
        // 1. Create Transfer Request/Order
        InterWarehouseTransferItemRequest itemReq = new InterWarehouseTransferItemRequest(
                product.getId(),
                srcLoc.getId(),
                destLoc.getId(),
                new BigDecimal("30.00")
        );

        InterWarehouseTransferCreateRequest createReq = new InterWarehouseTransferCreateRequest(
                "EXT-12345",
                srcWarehouse.getId(),
                destWarehouse.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "Transfer note",
                List.of(itemReq)
        );

        InterWarehouseTransferResponse trf = transferService.createTransfer(createReq, planner);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.NEW);

        // 2. Approve Transfer (Manager action)
        trf = transferService.approveTransfer(trf.id(), manager);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.APPROVED);

        // 3. Assign Trip
        InterWarehouseTransferTripAssignRequest tripReq = new InterWarehouseTransferTripAssignRequest(
                vehicle.getId(),
                driver.getId(),
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(5)
        );

        trf = transferService.assignTrip(trf.id(), tripReq, planner);
        // assignTrip keeps status APPROVED, but sets trip
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.APPROVED);
        assertThat(trf.tripId()).isNotNull();

        // 4. Outbound QC and load handover with photo confirmation
        trf = transferService.recordOutboundQc(
                trf.id(),
                new OutboundQcRequest(true, "Outbound QC passed", "transfer/outbound-qc/trf-001.jpg"),
                storekeeper
        );
        assertThat(trf.outboundQcPassed()).isTrue();

        trf = transferService.loadHandover(
                trf.id(),
                new LoadHandoverRequest("transfer/load-handover/trf-001.jpg"),
                storekeeper
        );
        assertThat(trf.loadHandoverPhotoRef()).isEqualTo("transfer/load-handover/trf-001.jpg");

        // 5. Ship Transfer (keeps status APPROVED)
        trf = transferService.shipTransfer(trf.id(), storekeeper);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.APPROVED);

        // 6. Depart Transfer (driver departs) - transitions status to IN_TRANSIT
        trf = transferService.departTransfer(trf.id(), driverUser);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);

        // Verify source inventory deducted, transit inventory created/updated after departure
        Inventory updatedSrc = inventoryRepository.findById(srcInventory.getId()).orElseThrow();
        assertThat(updatedSrc.getTotalQty()).isEqualByComparingTo(new BigDecimal("70.00"));

        Inventory transitInv = inventoryRepository.findTransitRowForDeliveryConfirmation(product.getId(), batch.getId())
                .orElseThrow();
        assertThat(transitInv.getTotalQty()).isEqualByComparingTo(new BigDecimal("30.00"));

        // 7. Driver arrival and destination handover
        trf = transferService.driverArrive(trf.id(), driverUser);
        assertThat(trf.driverArrivedAt()).isNotNull();

        trf = transferService.receivingHandover(
                trf.id(),
                new LoadHandoverRequest("transfer/arrival-handover/trf-001.jpg"),
                storekeeper
        );
        assertThat(trf.arrivalHandoverAt()).isNotNull();
        assertThat(trf.arrivalHandoverPhotoRef()).isEqualTo("transfer/arrival-handover/trf-001.jpg");

        // 8. Receive Count at destination
        InterWarehouseTransferReceiveCountItemRequest countItem = new InterWarehouseTransferReceiveCountItemRequest(
                trf.items().get(0).id(),
                new BigDecimal("30.00"),
                ""
        );
        InterWarehouseTransferReceiveCountRequest countReq = new InterWarehouseTransferReceiveCountRequest(List.of(countItem));

        trf = transferService.receiveCount(trf.id(), countReq, storekeeper);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);

        // 9. Receive Check (QC verification) at destination
        InterWarehouseTransferReceiveCheckItemRequest checkItem = new InterWarehouseTransferReceiveCheckItemRequest(
                trf.items().get(0).id(),
                new BigDecimal("30.00"),
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                destLoc.getId(),
                "",
                ""
        );
        InterWarehouseTransferReceiveCheckRequest checkReq = new InterWarehouseTransferReceiveCheckRequest(List.of(checkItem));

        trf = transferService.receiveCheck(trf.id(), checkReq, storekeeper);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);

        // 10. Final Receive (completes the transfer, moves stock from transit bin to destination bin)
        InterWarehouseTransferFinalReceiveRequest finalReq = new InterWarehouseTransferFinalReceiveRequest("Completed successfully");
        trf = transferService.finalReceive(trf.id(), finalReq, manager);
        assertThat(trf.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED);

        // Verify destination stock
        Inventory destInventory = inventoryRepository
                .findByWarehouseProductBatchLocation(destWarehouse.getId(), product.getId(), batch.getId(), destLoc.getId())
                .orElseThrow();
        assertThat(destInventory.getTotalQty()).isEqualByComparingTo(new BigDecimal("30.00"));

        // Verify transit stock is zero
        Inventory updatedTransit = inventoryRepository.findById(transitInv.getId()).orElseThrow();
        assertThat(updatedTransit.getTotalQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
