package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferFinalReceiveRequest;
import com.wms.dto.request.InterWarehouseTransferItemRequest;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCheckItemRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCheckRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCountItemRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCountRequest;
import com.wms.dto.request.InterWarehouseTransferTripAssignRequest;
import com.wms.dto.request.InterWarehouseTransferUpdateRequest;
import com.wms.dto.request.InterWarehouseTransferRejectRequest;
import com.wms.dto.request.TransferReturnRequest;
import com.wms.dto.request.TransferReturnRejectRequest;
import com.wms.dto.request.LoadHandoverRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.Batch;
import com.wms.entity.Driver;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.entity.InterWarehouseTransfer;
import com.wms.entity.InterWarehouseTransferAllocation;
import com.wms.entity.InterWarehouseTransferItem;
import com.wms.entity.Trip;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.DriverStatus;
import com.wms.enums.InterWarehouseTransferStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.UserRole;
import com.wms.enums.VehicleStatus;
import com.wms.enums.LocationType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.DriverRepository;
import com.wms.repository.QuarantineRecordRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InterWarehouseTransferAllocationRepository;
import com.wms.repository.InterWarehouseTransferItemRepository;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.transfer.impl.*;
import com.wms.mapper.InterWarehouseTransferMapper;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterWarehouseTransferServiceImplTest {

    private static final LocalDateTime VALID_TRIP_START = LocalDate.now().plusDays(8).atTime(9, 0);
    private static final LocalDateTime VALID_TRIP_END = LocalDate.now().plusDays(8).atTime(12, 0);

    private InterWarehouseTransferRepository transferRepository;
    private InterWarehouseTransferItemRepository transferItemRepository;
    private InterWarehouseTransferAllocationRepository allocationRepository;
    private InventoryRepository inventoryRepository;
    private WarehouseRepository warehouseRepository;
    private WarehouseLocationRepository locationRepository;
    private UserWarehouseAssignmentRepository assignmentRepository;
    private VehicleRepository vehicleRepository;
    private DriverRepository driverRepository;
    private TripRepository tripRepository;
    private AdjustmentRepository adjustmentRepository;
    private QuarantineRecordRepository quarantineRecordRepository;
    private com.wms.repository.DiscrepancyIncidentRepository discrepancyIncidentRepository;
    private com.wms.repository.DiscrepancyHoldEntryRepository discrepancyHoldEntryRepository;
    private com.wms.repository.ProductRepository productRepository;
    private com.wms.repository.WrongSkuReportRepository wrongSkuReportRepository;
    private com.wms.repository.WrongSkuReportItemRepository wrongSkuReportItemRepository;
    private TrackingAuditUtil auditUtil;
    private EntityManager entityManager;
    private InterWarehouseTransferServiceImpl service;

    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;
    private Warehouse transitWarehouse;
    private WarehouseLocation sourceLocation;
    private WarehouseLocation transitLocation;
    private WarehouseLocation destinationLocation;
    private WarehouseLocation quarantineLocation;
    private Product product;
    private User planner;
    private User sourceManager;
    private User destinationWorker;
    private User destinationStorekeeper;
    private User destinationManager;
    private User dispatcher;
    private User driverUser;
    private Vehicle vehicle;
    private Driver driver;
    private InterWarehouseTransfer transfer;
    private InterWarehouseTransferItem transferItem;
    private final List<InterWarehouseTransferItem> transferItems = new ArrayList<>();
    private Inventory sourceInventory;
    private Inventory transitInventory;
    private Inventory destinationInventory;
    private Inventory quarantineInventory;
    private Trip transferTrip;
    private final Map<Long, List<Long>> assignments = new HashMap<>();
    private final TrackingAllocationRepository allocationState = new TrackingAllocationRepository();

    @BeforeEach
    void setUp() {
        sourceWarehouse = warehouse(1L, "HP-01");
        destinationWarehouse = warehouse(2L, "HN-01");
        transitWarehouse = warehouse(99L, "IN_TRANSIT");
        sourceLocation = location(10L, sourceWarehouse, "HP-01-B01", false);
        transitLocation = location(11L, transitWarehouse, "INT-01", false);
        destinationLocation = location(12L, destinationWarehouse, "HN-01-B01", false);
        quarantineLocation = location(13L, destinationWarehouse, "HN-01-Q01", true);
        product = product();
        planner = user(7L, UserRole.PLANNER);
        sourceManager = user(8L, UserRole.WAREHOUSE_MANAGER);
        destinationWorker = user(9L, UserRole.WAREHOUSE_STAFF);
        destinationStorekeeper = user(10L, UserRole.STOREKEEPER);
        destinationManager = user(11L, UserRole.WAREHOUSE_MANAGER);
        dispatcher = user(13L, UserRole.DISPATCHER);
        driverUser = user(12L, UserRole.DRIVER);
        vehicle = vehicle();
        driver = driver();
        transfer = transfer();
        transferItem = transferItem();
        transferItems.clear();
        transferItems.add(transferItem);
        sourceInventory = inventory(sourceWarehouse, sourceLocation, new BigDecimal("20.00"));
        transitInventory = null;
        destinationInventory = null;
        quarantineInventory = null;
        transferTrip = null;

        assignments.clear();
        assignments.put(sourceManager.getId(), List.of(sourceWarehouse.getId()));
        assignments.put(destinationWorker.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(destinationStorekeeper.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(destinationManager.getId(), List.of(destinationWarehouse.getId()));
        assignments.put(dispatcher.getId(), List.of(sourceWarehouse.getId()));
        assignments.put(planner.getId(), List.of(sourceWarehouse.getId()));
        assignments.put(driverUser.getId(), List.of(sourceWarehouse.getId(), destinationWarehouse.getId()));

        transferRepository = proxy(InterWarehouseTransferRepository.class, new TransferRepoHandler());
        transferItemRepository = proxy(InterWarehouseTransferItemRepository.class, new TransferItemRepoHandler());
        allocationRepository = proxy(InterWarehouseTransferAllocationRepository.class, allocationState);
        inventoryRepository = proxy(InventoryRepository.class, new InventoryRepoHandler());
        warehouseRepository = proxy(WarehouseRepository.class, new WarehouseRepoHandler());
        locationRepository = proxy(WarehouseLocationRepository.class, new LocationRepoHandler());
        assignmentRepository = proxy(UserWarehouseAssignmentRepository.class, new AssignmentRepoHandler());
        vehicleRepository = proxy(VehicleRepository.class, new VehicleRepoHandler());
        driverRepository = proxy(DriverRepository.class, new DriverRepoHandler());
        tripRepository = proxy(TripRepository.class, new TripRepoHandler());
        adjustmentRepository = proxy(AdjustmentRepository.class, new AdjustmentRepoHandler());
        quarantineRecordRepository = proxy(QuarantineRecordRepository.class, new QuarantineRecordRepoHandler());
        discrepancyIncidentRepository = proxy(com.wms.repository.DiscrepancyIncidentRepository.class, new DefaultRepoHandler());
        discrepancyHoldEntryRepository = proxy(com.wms.repository.DiscrepancyHoldEntryRepository.class, new DefaultRepoHandler());
        productRepository = proxy(com.wms.repository.ProductRepository.class, new ProductRepoHandler());
        wrongSkuReportRepository = proxy(com.wms.repository.WrongSkuReportRepository.class, new DefaultRepoHandler());
        wrongSkuReportItemRepository = proxy(com.wms.repository.WrongSkuReportItemRepository.class, new DefaultRepoHandler());
        auditUtil = new TrackingAuditUtil();
        entityManager = proxy(EntityManager.class, new EntityManagerHandler());

        InterWarehouseTransferMapper mapper = new InterWarehouseTransferMapper();

        InterWarehouseTransferHelper helper = new InterWarehouseTransferHelper(
                transferRepository, transferItemRepository, allocationRepository,
                inventoryRepository, locationRepository, assignmentRepository,
                tripRepository, mapper, auditUtil, entityManager);

        InterWarehouseTransferPlanningService planningService = new InterWarehouseTransferPlanningService(
                transferRepository, transferItemRepository, helper);

        InterWarehouseTransferApprovalService approvalService = new InterWarehouseTransferApprovalService(
                transferRepository, helper);

        InterWarehouseTransferShippingService shippingService = new InterWarehouseTransferShippingService(
                transferRepository, transferItemRepository, allocationRepository,
                inventoryRepository, warehouseRepository, locationRepository,
                assignmentRepository, vehicleRepository, driverRepository,
                tripRepository, helper);

        InterWarehouseTransferReceivingService receivingService = new InterWarehouseTransferReceivingService(
                transferRepository, transferItemRepository, allocationRepository,
                inventoryRepository, warehouseRepository, locationRepository,
                adjustmentRepository, auditUtil, helper, quarantineRecordRepository,
                discrepancyIncidentRepository, discrepancyHoldEntryRepository,
                productRepository, wrongSkuReportRepository, wrongSkuReportItemRepository);

        service = new InterWarehouseTransferServiceImpl(
                transferRepository, helper, planningService,
                approvalService, shippingService, receivingService);
    }

    @Test
    void plannerLifecycle_createUpdateCancelNewWorks() {
        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "CTM-20260617-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 18),
                "manual instruction",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("4.00"))));

        InterWarehouseTransferResponse created = service.createTransfer(createRequest, planner);
        assertThat(created.status()).isEqualTo(InterWarehouseTransferStatus.NEW);
        assertThat(created.externalInstructionCode()).isEqualTo("CTM-20260617-01");

        InterWarehouseTransferUpdateRequest updateRequest = new InterWarehouseTransferUpdateRequest(
                "CTM-20260617-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 19),
                "manual instruction updated",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("6.00"))));
        InterWarehouseTransferResponse updated = service.updateTransfer(1L, updateRequest, planner);
        assertThat(updated.plannedDate()).isEqualTo(LocalDate.of(2026, 6, 19));
        assertThat(updated.items()).hasSize(1);
        assertThat(updated.items().get(0).plannedQty()).isEqualByComparingTo("6.00");

        InterWarehouseTransferResponse cancelled = service.cancelTransfer(1L,
                new InterWarehouseTransferReasonRequest("Planner cancel"), planner);
        assertThat(cancelled.status()).isEqualTo(InterWarehouseTransferStatus.CANCELLED);
    }

    @Test
    void plannerLifecycle_outsideWarehouseScope_createUpdateCancelFails() {
        User unassignedPlanner = user(999L, UserRole.PLANNER);
        assignments.put(unassignedPlanner.getId(), List.of());

        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "CTM-OUTSIDE-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 18),
                "outside scope",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("4.00"))));

        assertThatThrownBy(() -> service.createTransfer(createRequest, unassignedPlanner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        InterWarehouseTransferUpdateRequest updateRequest = new InterWarehouseTransferUpdateRequest(
                "CTM-OUTSIDE-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.of(2026, 6, 17),
                LocalDate.of(2026, 6, 19),
                "outside scope updated",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("6.00"))));

        assertThatThrownBy(() -> service.updateTransfer(1L, updateRequest, unassignedPlanner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        assertThatThrownBy(() -> service.cancelTransfer(1L,
                new InterWarehouseTransferReasonRequest("Outside scope cancel"), unassignedPlanner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");
    }

    @Test
    void canViewTransfer_plannerFiltersSuccessfully() {
        assertThat(service.getAllTransfers(planner)).hasSize(1);

        User otherPlanner = user(888L, UserRole.PLANNER);
        assignments.put(otherPlanner.getId(), List.of(3L));

        assertThat(service.getAllTransfers(otherPlanner)).isEmpty();

        assertThatThrownBy(() -> service.getTransferById(1L, otherPlanner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");
    }

    @Test
    void sourceFlow_approveAssignShipUnshipDepartWorks() {
        transfer.setCreatedBy(planner);
        InterWarehouseTransferResponse approved = service.approveTransfer(1L, sourceManager);
        assertThat(approved.status()).isEqualTo(InterWarehouseTransferStatus.APPROVED);
        assertThat(sourceInventory.getReservedQty()).isEqualByComparingTo("5.00");
        assertThat(allocationState.saved).hasSize(1);
        assertThat(auditUtil.lastAction).isEqualTo(AuditAction.TRANSFER_APPROVE);

        InterWarehouseTransferResponse assigned = service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(
                vehicle.getId(), driver.getId(), VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        assertThat(assigned.tripId()).isNotNull();
        assertThat(transfer.getTrip()).isNotNull();
        assertThat(assigned.tripPlannedStartAt()).isEqualTo(VALID_TRIP_START);
        assertThat(assigned.tripPlannedEndAt()).isEqualTo(VALID_TRIP_END);

        InterWarehouseTransferResponse shipped = service.shipTransfer(1L, sourceManager);
        assertThat(shipped.items().get(0).sentQty()).isEqualByComparingTo("5.00");

        InterWarehouseTransferResponse unshipped = service.unshipTransfer(1L, sourceManager);
        assertThat(unshipped.items().get(0).sentQty()).isNull();

        service.shipTransfer(1L, sourceManager);
        InterWarehouseTransferResponse departed = service.departTransfer(1L, driverUser);
        assertThat(departed.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
        assertThat(sourceInventory.getTotalQty()).isEqualByComparingTo("15.00");
        assertThat(transitInventory).isNotNull();
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(transfer.getTrip().getStatus()).isEqualTo(TripStatus.IN_TRANSIT);
    }

    @Test
    void assignTrip_requiresDriverFromSourceWarehouse() {
        service.approveTransfer(1L, sourceManager);
        assignments.put(driverUser.getId(), List.of(destinationWarehouse.getId()));

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_SOURCE_WAREHOUSE_REQUIRED");
    }

    @Test
    void assignTrip_requiresDispatcherAndVehicleFromSourceWarehouse() {
        service.approveTransfer(1L, sourceManager);

        assignments.put(dispatcher.getId(), List.of(destinationWarehouse.getId()));
        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        assignments.put(dispatcher.getId(), List.of(sourceWarehouse.getId()));
        vehicle.setWarehouse(destinationWarehouse);
        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("VEHICLE_SOURCE_WAREHOUSE_REQUIRED");
    }

    @Test
    void destinationWorker_onlySeesTransfersForAssignedWarehouses() {
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);

        assertThat(service.getAllTransfers(destinationWorker)).hasSize(1);

        assignments.put(destinationWorker.getId(), List.of(999L));
        assertThat(service.getAllTransfers(destinationWorker)).isEmpty();
        assertThatThrownBy(() -> service.getTransferById(transfer.getId(), destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");
    }

    @Test
    void destinationFlow_receiveCountCheckAndFinalConfirmWorks() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        InterWarehouseTransferResponse counted = service.receiveCount(1L,
                new InterWarehouseTransferReceiveCountRequest(List.of(
                        new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("4.00"),
                                "1 missing"))),
                destinationWorker);
        assertThat(counted.items().get(0).workerReceivedQty()).isEqualByComparingTo("4.00");

        InterWarehouseTransferResponse checked = service.receiveCheck(1L,
                new InterWarehouseTransferReceiveCheckRequest(List.of(
                        new InterWarehouseTransferReceiveCheckItemRequest(
                                transferItem.getId(),
                                new BigDecimal("4.00"),
                                new BigDecimal("3.00"),
                                new BigDecimal("1.00"),
                                destinationLocation.getId(),
                                "checker adjusted count",
                                "one damaged"))),
                destinationStorekeeper);
        assertThat(checked.items().get(0).receivedQty()).isEqualByComparingTo("4.00");
        assertThat(checked.items().get(0).qcPassedQty()).isEqualByComparingTo("3.00");

        InterWarehouseTransferResponse completed = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("shortage due to missing unit"), destinationManager);
        assertThat(completed.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY);
        assertThat(destinationInventory).isNotNull();
        assertThat(destinationInventory.getTotalQty()).isEqualByComparingTo("3.00");
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("1.00");
    }

    @Test
    void receiveCount_overReceipt_isAllowedAndRoutedToHold() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        // Over-receipt count of 6.00 is allowed now (sent was 5.00)
        InterWarehouseTransferResponse counted = service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("6.00"),
                        "extra item found"))),
                destinationWorker);
        assertThat(counted.items().get(0).workerReceivedQty()).isEqualByComparingTo("6.00");
    }

    @Test
    void assignTrip_requiresValidScheduleWindow() {
        service.approveTransfer(1L, sourceManager);

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_START),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TRIP_SCHEDULE_INVALID");
    }

    @Test
    void assignTrip_throwsWhenStartInPast() {
        service.approveTransfer(1L, sourceManager);
        LocalDateTime pastStart = LocalDateTime.now().minusMinutes(20);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), pastStart, end),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TRIP_START_IN_PAST");
    }

    @Test
    void assignTrip_throwsWhenEndInPast() {
        service.approveTransfer(1L, sourceManager);
        LocalDateTime start = LocalDateTime.now().minusMinutes(10);
        LocalDateTime pastEnd = LocalDateTime.now().minusMinutes(2);

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), start, pastEnd),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TRIP_END_IN_PAST");
    }

    @Test
    void returnToSource_setsIsReturnedTrueAndRestrictsReceivingToSourceWarehouse() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        // Driver must be blocked
        TransferReturnRequest req = new TransferReturnRequest("Overdue return");
        assertThatThrownBy(() -> service.returnToSource(1L, req, driverUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_MANAGER_ROLE_REQUIRED");

        // Destination Manager must be blocked
        assertThatThrownBy(() -> service.returnToSource(1L, req, destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        // Source Manager succeeds
        InterWarehouseTransferResponse response = service.returnToSource(1L, req, sourceManager);
        assertThat(response.isReturned()).isTrue();

        // T058: Execute return leg steps: driver departs, arrives, and hands over back to source warehouse
        service.returnDepart(1L, driverUser);
        service.returnArrive(1L, driverUser);

        User sourceWorker = user(999L, UserRole.WAREHOUSE_STAFF);
        assignments.put(sourceWorker.getId(), List.of(sourceWarehouse.getId()));

        service.returnHandover(1L, new LoadHandoverRequest("return_handover.jpg"), sourceWorker);

        assertThatThrownBy(() -> service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"),
                        "returning"))),
                destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        InterWarehouseTransferResponse counted = service.receiveCount(1L,
                new InterWarehouseTransferReceiveCountRequest(List.of(
                        new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"),
                                "shortage during return"))),
                sourceWorker);
        assertThat(counted.items().get(0).workerReceivedQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void quarantineReject_storekeeper_success() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("Storekeeper found completely broken boxes on arrival");

        InterWarehouseTransferResponse response = service.quarantineReject(1L, request, destinationStorekeeper);

        assertThat(response.status()).isEqualTo(InterWarehouseTransferStatus.QUARANTINED);
        assertThat(response.rejectionReason()).isEqualTo("Storekeeper found completely broken boxes on arrival");
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void quarantineReject_manager_success() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);

        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(
                List.of(new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        destinationLocation.getId(),
                        "Everything counted",
                        null))),
                destinationStorekeeper);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("Manager rejected due to administrative discrepancy");

        InterWarehouseTransferResponse response = service.quarantineReject(1L, request, destinationManager);

        assertThat(response.status()).isEqualTo(InterWarehouseTransferStatus.QUARANTINED);
        assertThat(response.rejectionReason()).isEqualTo("Manager rejected due to administrative discrepancy");
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void quarantineReject_failsIfReasonBlank() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("");

        assertThatThrownBy(() -> service.quarantineReject(1L, request, destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("REJECTION_REASON_REQUIRED");
    }

    @Test
    void receiving_blocksIfArriveOrHandoverMissing() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        // Driver Arrive and Handover are NULL by default in this test because we clear them
        transfer.setDriverArrivedAt(null);
        transfer.setArrivalHandoverAt(null);

        assertThatThrownBy(() -> service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_ARRIVE_REQUIRED");

        transfer.setDriverArrivedAt(OffsetDateTime.now());

        assertThatThrownBy(() -> service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ARRIVAL_HANDOVER_REQUIRED");
    }

    @Test
    void receiving_blocksIfBinCapacityExceeded() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        // Set bin capacity to be extremely small (0.01 m3)
        destinationLocation.setCapacityM3(new BigDecimal("0.01"));
        destinationLocation.setCurrentVolumeM3(BigDecimal.ZERO);
        // Make product large (1.00 m3)
        product.setVolumeM3(new BigDecimal("1.00"));

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);

        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(
                List.of(new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        destinationLocation.getId(),
                        "Check ok",
                        null))),
                destinationStorekeeper);

        // finalReceive should throw BIN_CAPACITY_EXCEEDED because 5.00 * 1.00 > 0.01
        assertThatThrownBy(() -> service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest("final"), destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("BIN_CAPACITY_EXCEEDED");
    }

    @Test
    void rejectReturn_rejectsPendingWrongSkuReports() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        // 1. Request return first
        TransferReturnRequest req = new TransferReturnRequest("Giao sai mã SKU chảo");
        service.requestReturn(1L, req, destinationManager);

        assertThat(transfer.isReturnRequested()).isTrue();

        // 2. Destination Manager rejects return
        TransferReturnRejectRequest rejectReq = new TransferReturnRejectRequest("Rejecting because wrong SKU claims are incorrect");
        service.rejectReturn(1L, rejectReq, destinationManager);

        assertThat(transfer.isReturnRequested()).isFalse();
        assertThat(transfer.getReturnRejectionReason()).isEqualTo("Rejecting because wrong SKU claims are incorrect");
    }

    private InterWarehouseTransfer transfer() {
        InterWarehouseTransfer value = new InterWarehouseTransfer();
        value.setId(1L);
        value.setTransferNumber("TRF-20260617-0001");
        value.setExternalInstructionCode("CTM-0001");
        value.setSourceWarehouse(sourceWarehouse);
        value.setDestinationWarehouse(destinationWarehouse);
        value.setStatus(InterWarehouseTransferStatus.NEW);
        value.setOutboundQcPassed(true);
        value.setLoadHandoverPhotoRef("photo.jpg");
        value.setDriverArrivedAt(OffsetDateTime.now());
        value.setArrivalHandoverAt(OffsetDateTime.now());
        value.setCreatedBy(planner);
        value.setDocumentDate(LocalDate.of(2026, 6, 17));
        value.setCreatedAt(OffsetDateTime.now());
        value.setUpdatedAt(OffsetDateTime.now());
        return value;
    }

    private InterWarehouseTransferItem transferItem() {
        InterWarehouseTransferItem item = new InterWarehouseTransferItem();
        item.setId(101L);
        item.setTransfer(transfer);
        item.setProduct(product);
        item.setSourceLocation(sourceLocation);
        item.setDestinationLocation(destinationLocation);
        item.setPlannedQty(new BigDecimal("5.00"));
        return item;
    }

    private Inventory inventory(Warehouse warehouse, WarehouseLocation location, BigDecimal totalQty) {
        Batch batch = new Batch();
        batch.setId(301L);
        Inventory value = new Inventory();
        value.setId(401L);
        value.setWarehouse(warehouse);
        value.setProduct(product);
        value.setBatch(batch);
        value.setLocation(location);
        value.setTotalQty(totalQty);
        value.setReservedQty(BigDecimal.ZERO);
        value.setCostPrice(new BigDecimal("100.00"));
        value.setVersion(0);
        value.setUpdatedAt(OffsetDateTime.now());
        return value;
    }

    private Product product() {
        Product value = new Product();
        value.setId(21L);
        value.setSku("SKU-001");
        value.setName("Nồi inox");
        return value;
    }

    private Warehouse warehouse(Long id, String code) {
        Warehouse value = new Warehouse();
        value.setId(id);
        value.setCode(code);
        value.setName(code);
        return value;
    }

    private WarehouseLocation location(Long id, Warehouse warehouse, String code, boolean quarantine) {
        WarehouseLocation value = new WarehouseLocation();
        value.setId(id);
        value.setWarehouse(warehouse);
        value.setCode(code);
        value.setIsQuarantine(quarantine);
        value.setIsActive(true);
        value.setType(LocationType.BIN);
        return value;
    }

    private User user(Long id, UserRole role) {
        User value = new User();
        value.setId(id);
        value.setRole(role);
        return value;
    }

    private Vehicle vehicle() {
        Vehicle value = new Vehicle();
        value.setId(501L);
        value.setPlateNumber("15C-234.56");
        value.setVehicleType("Truck");
        value.setMaxWeightKg(new BigDecimal("1500.00"));
        value.setWarehouse(sourceWarehouse);
        value.setStatus(VehicleStatus.AVAILABLE);
        value.setIsActive(true);
        return value;
    }

    private Driver driver() {
        Driver value = new Driver();
        value.setId(601L);
        value.setUser(driverUser);
        value.setFullName("Driver A");
        value.setLicenseNumber("DRV-001");
        value.setLicenseExpiry(LocalDate.of(2029, 12, 31));
        value.setStatus(DriverStatus.AVAILABLE);
        value.setIsActive(true);
        return value;
    }

    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler));
    }

    private final class TransferRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findWithDetailsById" -> Optional.of(transfer);
                case "save" -> args[0];
                case "existsByTransferNumber" -> false;
                case "existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn" ->
                    false;
                case "existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot" ->
                    false;
                case "findAllByOrderByCreatedAtDesc" -> List.of(transfer);
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class TransferItemRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByTransferIdOrderById" -> List.copyOf(transferItems);
                case "save" -> {
                    InterWarehouseTransferItem item = (InterWarehouseTransferItem) args[0];
                    if (item.getId() == null) {
                        item.setId(transferItem.getId());
                    }
                    transferItems.clear();
                    transferItems.add(item);
                    transferItem = item;
                    yield item;
                }
                case "deleteByTransferId" -> {
                    transferItems.clear();
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class InventoryRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findReservableForUpdate" -> List.of(sourceInventory);
                case "findByIdForUpdate" -> Optional.of(findInventoryById((Long) args[0]));
                case "findByStockKeyForUpdate" -> Optional
                        .ofNullable(findInventoryByKey((Long) args[0], (Long) args[1], (Long) args[2], (Long) args[3]));
                case "save" -> saveInventory((Inventory) args[0]);
                default -> defaultValue(method.getReturnType());
            };
        }

        private Inventory findInventoryById(Long id) {
            if (sourceInventory != null && sourceInventory.getId().equals(id))
                return sourceInventory;
            if (transitInventory != null && transitInventory.getId().equals(id))
                return transitInventory;
            if (destinationInventory != null && destinationInventory.getId().equals(id))
                return destinationInventory;
            if (quarantineInventory != null && quarantineInventory.getId().equals(id))
                return quarantineInventory;
            throw new ResourceNotFoundException("Inventory not found");
        }

        private Inventory findInventoryByKey(Long warehouseId, Long productId, Long batchId, Long locationId) {
            if (matches(sourceInventory, warehouseId, productId, batchId, locationId))
                return sourceInventory;
            if (matches(transitInventory, warehouseId, productId, batchId, locationId))
                return transitInventory;
            if (matches(destinationInventory, warehouseId, productId, batchId, locationId))
                return destinationInventory;
            if (matches(quarantineInventory, warehouseId, productId, batchId, locationId))
                return quarantineInventory;
            return null;
        }

        private boolean matches(Inventory inventory, Long warehouseId, Long productId, Long batchId, Long locationId) {
            return inventory != null
                    && inventory.getWarehouse().getId().equals(warehouseId)
                    && inventory.getProduct().getId().equals(productId)
                    && inventory.getBatch().getId().equals(batchId)
                    && inventory.getLocation().getId().equals(locationId);
        }

        private Inventory saveInventory(Inventory value) {
            if (value.getWarehouse().getId().equals(sourceWarehouse.getId())) {
                sourceInventory = value;
            } else if (value.getWarehouse().getId().equals(transitWarehouse.getId())) {
                transitInventory = value;
            } else if (value.getLocation().getIsQuarantine()) {
                quarantineInventory = value;
            } else {
                destinationInventory = value;
            }
            if (value.getId() == null) {
                value.setId(900L);
            }
            return value;
        }
    }

    private final class WarehouseRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findByCode".equals(method.getName())) {
                return Optional.of(transitWarehouse);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class LocationRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByWarehouseIdAndTypeAndIsActiveTrue" -> List.of(transitLocation);
                case "findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue" -> List.of(quarantineLocation);
                case "findById" -> {
                    Long id = (Long) args[0];
                    if (destinationLocation.getId().equals(id))
                        yield Optional.of(destinationLocation);
                    if (quarantineLocation.getId().equals(id))
                        yield Optional.of(quarantineLocation);
                    if (transitLocation.getId().equals(id))
                        yield Optional.of(transitLocation);
                    if (sourceLocation.getId().equals(id))
                        yield Optional.of(sourceLocation);
                    yield Optional.empty();
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class AssignmentRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findWarehouseIdsByUserId".equals(method.getName())) {
                return assignments.getOrDefault((Long) args[0], List.of());
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class VehicleRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.of(vehicle);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class DriverRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.of(driver);
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class TripRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByTripNumber" -> false;
                case "existsVehicleScheduleOverlap", "existsDriverScheduleOverlap" -> false;
                case "save" -> {
                    transferTrip = (Trip) args[0];
                    if (transferTrip.getId() == null) {
                        transferTrip.setId(701L);
                    }
                    yield transferTrip;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class EntityManagerHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getReference".equals(method.getName()) && args.length == 2) {
                Class<?> type = (Class<?>) args[0];
                Long id = (Long) args[1];
                if (type == Warehouse.class) {
                    if (sourceWarehouse.getId().equals(id))
                        return sourceWarehouse;
                    if (destinationWarehouse.getId().equals(id))
                        return destinationWarehouse;
                    if (transitWarehouse.getId().equals(id))
                        return transitWarehouse;
                }
                if (type == Product.class) {
                    return product;
                }
                if (type == WarehouseLocation.class) {
                    if (sourceLocation.getId().equals(id))
                        return sourceLocation;
                    if (destinationLocation.getId().equals(id))
                        return destinationLocation;
                    if (transitLocation.getId().equals(id))
                        return transitLocation;
                    if (quarantineLocation.getId().equals(id))
                        return quarantineLocation;
                }
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class TrackingAuditUtil extends PartnerAuditUtil {
        private AuditAction lastAction;

        TrackingAuditUtil() {
            super(null);
        }

        @Override
        public void logChange(User actor, AuditAction action, String entityType, Long entityId, String entityCode,
                Map<String, Object> before, Map<String, Object> after) {
            lastAction = action;
        }
    }

    private final class TrackingAllocationRepository implements InvocationHandler {
        private final List<InterWarehouseTransferAllocation> saved = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "save" -> {
                    saved.add((InterWarehouseTransferAllocation) args[0]);
                    yield args[0];
                }
                case "findByTransferItemTransferId", "findByTransferItemId" -> saved;
                case "deleteByTransferItemTransferId" -> {
                    saved.clear();
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
        }
    }

    private final class AdjustmentRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                return args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class QuarantineRecordRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                return args[0];
            }
            return defaultValue(method.getReturnType());
        }
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == double.class || type == float.class) {
            return 0.0;
        }
        if (type == void.class) {
            return null;
        }
        return null;
    }

    private final class DefaultRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                return args[0];
            }
            if ("findByTransferId".equals(method.getName())) {
                return new ArrayList<>();
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class ProductRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("findById".equals(method.getName())) {
                return Optional.of(product);
            }
            return defaultValue(method.getReturnType());
        }
    }
}
