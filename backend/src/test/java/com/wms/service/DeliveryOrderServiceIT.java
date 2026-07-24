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

import com.wms.dto.request.CreateReceiptRequest;
import com.wms.dto.request.DeliveryOrderCreateRequest;
import com.wms.dto.request.DeliveryOrderItemCreateRequest;
import com.wms.dto.request.DeliveryOrderPickingPlanRequest;
import com.wms.dto.request.DeliveryOrderPickQcResultRequest;
import com.wms.dto.request.DeliveryOrderPickQcRowRequest;
import com.wms.dto.request.DeliveryOrderQualityApprovalRequest;
import com.wms.dto.request.DeliveryOrderWarehouseApprovalRequest;
import com.wms.dto.response.DeliveryOrderResponse;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.access_control.User;
import com.wms.entity.access_control.UserWarehouseAssignment;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.entity.stock_control.WarehouseProductReservation;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderType;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.repository.BatchRepository;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseProductReservationRepository;
import com.wms.repository.WarehouseRepository;
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
    "spring.datasource.url=jdbc:h2:mem:outboundtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
public class DeliveryOrderServiceIT {

    @Autowired
    private DeliveryOrderService deliveryOrderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DealerRepository dealerRepository;

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
    private PriceHistoryRepository priceHistoryRepository;

    @Autowired
    private WarehouseProductReservationRepository reservationRepository;

    @Autowired
    private UserWarehouseAssignmentRepository assignmentRepository;

    @Autowired
    private com.wms.repository.DeliveryOrderItemAllocationRepository allocationRepository;

    private User planner;
    private User storekeeper;
    private User staff;
    private User manager;
    private Dealer dealer;
    private Warehouse warehouse;
    private WarehouseLocation binLoc;
    private WarehouseLocation stagingLoc;
    private Product product;
    private Batch batch;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        batchRepository.deleteAll();
        priceHistoryRepository.deleteAll();
        reservationRepository.deleteAll();
        assignmentRepository.deleteAll();
        locationRepository.deleteAll();
        productRepository.deleteAll();
        dealerRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Setup users
        planner = userRepository.save(User.builder()
                .code("PL001").fullName("Planner").email("plan@wms.com")
                .passwordHash("hash").role(UserRole.PLANNER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        storekeeper = userRepository.save(User.builder()
                .code("SK001").fullName("Storekeeper").email("store@wms.com")
                .passwordHash("hash").role(UserRole.STOREKEEPER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        staff = userRepository.save(User.builder()
                .code("ST001").fullName("Staff").email("staff@wms.com")
                .passwordHash("hash").role(UserRole.WAREHOUSE_STAFF).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        manager = userRepository.save(User.builder()
                .code("MG001").fullName("Manager").email("manager@wms.com")
                .passwordHash("hash").role(UserRole.WAREHOUSE_MANAGER).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 2. Setup Warehouse & Dealer
        warehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MAIN").name("Main Warehouse").type(WarehouseType.PHYSICAL).isActive(true)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        dealer = dealerRepository.save(Dealer.builder()
                .code("D001").name("Dealer 1").isActive(true).creditStatus(CreditStatus.ACTIVE)
                .creditLimit(new BigDecimal("100000.00")).currentBalance(BigDecimal.ZERO)
                .paymentTermDays(30).createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // Save assignments
        for (User u : List.of(planner, storekeeper, staff, manager)) {
            UserWarehouseAssignment assign = new UserWarehouseAssignment();
            assign.setUser(u);
            assign.setWarehouse(warehouse);
            assign.setAssignedBy(planner);
            assign.setAssignedAt(OffsetDateTime.now());
            assignmentRepository.save(assign);
        }

        // 3. Save locations
        binLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(warehouse).code("BIN-01").type(LocationType.BIN)
                .isQuarantine(false).isActive(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        stagingLoc = locationRepository.save(WarehouseLocation.builder()
                .warehouse(warehouse).code("STAGE-01").type(LocationType.BIN)
                .isQuarantine(false).isActive(true).isStaging(true)
                .currentVolumeM3(BigDecimal.ZERO).currentWeightKg(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        // 4. Save Product & Price
        product = productRepository.save(Product.builder()
                .sku("PROD-100").name("Chảo Chống Dính").unit("PCS")
                .isActive(true).weightKg(new BigDecimal("1.00")).volumeM3(new BigDecimal("0.01"))
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build());

        PriceHistory price = PriceHistory.builder()
                .product(product).warehouse(warehouse)
                .costPrice(new BigDecimal("50.00")).sellingPrice(new BigDecimal("99.00"))
                .effectiveDate(LocalDate.now().minusDays(5))
                .status(PriceHistoryStatus.APPROVED).createdBy(planner)
                .createdAt(OffsetDateTime.now()).build();
        priceHistoryRepository.save(price);

        // 5. Seed stock (Inventory)
        batch = batchRepository.save(Batch.builder()
                .batchNumber("BCH-100").product(product).warehouse(warehouse)
                .receivedDate(LocalDate.now().minusDays(10)).quantity(new BigDecimal("50.00"))
                .createdAt(OffsetDateTime.now()).build());

        inventory = inventoryRepository.save(Inventory.builder()
                .warehouse(warehouse).product(product).batch(batch).location(binLoc)
                .totalQty(new BigDecimal("50.00")).reservedQty(BigDecimal.ZERO)
                .costPrice(new BigDecimal("50.00")).updatedAt(OffsetDateTime.now()).build());
    }

    @Test
    void testDeliveryOrderFlow_completeLifecycle() {
        // 1. Create DO (status = NEW)
        DeliveryOrderCreateRequest createReq = new DeliveryOrderCreateRequest();
        createReq.setDealerId(dealer.getId());
        createReq.setWarehouseId(warehouse.getId());
        createReq.setType(DeliveryOrderType.SALE);
        createReq.setDocumentDate(LocalDate.now());

        DeliveryOrderItemCreateRequest itemReq = new DeliveryOrderItemCreateRequest();
        itemReq.setProductId(product.getId());
        itemReq.setRequestedQty(new BigDecimal("10.00"));
        createReq.setItems(List.of(itemReq));

        DeliveryOrderResponse doResp = deliveryOrderService.createDeliveryOrder(createReq, planner);
        assertThat(doResp.getStatus()).isEqualTo(DeliveryOrderStatus.NEW);
        assertThat(doResp.getItems()).hasSize(1);
        Long doItemId = doResp.getItems().get(0).getId();

        // Verify Planner Reservation created
        WarehouseProductReservation res = reservationRepository
                .findWithWarehouseAndProductByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseThrow();
        assertThat(res.getReservedQty()).isEqualByComparingTo(new BigDecimal("10.00"));

        // 2. Save Picking Plan (status becomes WAITING_PICKING)
        // With empty allocations list, the service auto-allocates based on FIFO candidates!
        DeliveryOrderPickingPlanRequest pickingPlanReq = new DeliveryOrderPickingPlanRequest();
        pickingPlanReq.setAllocations(List.of());

        doResp = deliveryOrderService.saveDeliveryOrderPickingPlan(doResp.getId(), pickingPlanReq, storekeeper);
        assertThat(doResp.getStatus()).isEqualTo(DeliveryOrderStatus.WAITING_PICKING);
        List<com.wms.entity.order_fulfillment.DeliveryOrderItemAllocation> allocations = allocationRepository
                .findByDeliveryOrderItemDeliveryOrderId(doResp.getId());
        assertThat(allocations).hasSize(1);
        Long allocationId = allocations.get(0).getId();

        // 3. Save Pick QC Results (status becomes QC_PENDING_APPROVAL)
        DeliveryOrderPickQcResultRequest qcResultReq = new DeliveryOrderPickQcResultRequest();
        DeliveryOrderPickQcRowRequest rowReq = new DeliveryOrderPickQcRowRequest();
        rowReq.setAllocationId(allocationId);
        rowReq.setDoItemId(doItemId);
        rowReq.setBatchId(batch.getId());
        rowReq.setLocationId(binLoc.getId());
        rowReq.setZoneId(binLoc.getId());
        rowReq.setPickedQty(new BigDecimal("10.00"));
        rowReq.setQcPassQty(new BigDecimal("10.00"));
        rowReq.setQcFailQty(BigDecimal.ZERO);
        rowReq.setStagingLocationId(stagingLoc.getId());
        qcResultReq.setResults(List.of(rowReq));

        doResp = deliveryOrderService.saveDeliveryOrderPickQcResult(doResp.getId(), qcResultReq, staff);
        assertThat(doResp.getStatus()).isEqualTo(DeliveryOrderStatus.QC_PENDING_APPROVAL);

        // 4. Quality Approval (status becomes QC_COMPLETED)
        DeliveryOrderQualityApprovalRequest qualityApproveReq = new DeliveryOrderQualityApprovalRequest();
        doResp = deliveryOrderService.approveDeliveryOrderQuality(doResp.getId(), qualityApproveReq, storekeeper);
        assertThat(doResp.getStatus()).isEqualTo(DeliveryOrderStatus.QC_COMPLETED);

        // 5. Warehouse Release Approval (status becomes WAREHOUSE_APPROVED)
        DeliveryOrderWarehouseApprovalRequest releaseReq = new DeliveryOrderWarehouseApprovalRequest();
        releaseReq.setNotes("Approved for delivery");
        doResp = deliveryOrderService.approveDeliveryOrderWarehouseRelease(doResp.getId(), releaseReq, manager);
        assertThat(doResp.getStatus()).isEqualTo(DeliveryOrderStatus.WAREHOUSE_APPROVED);
    }
}
