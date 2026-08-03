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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wms.dto.request.InterWarehouseTransferCreateRequest;
import com.wms.dto.request.InterWarehouseTransferFinalReceiveRequest;
import com.wms.dto.request.InterWarehouseTransferFinalPutawayItemRequest;
import com.wms.dto.request.InterWarehouseTransferItemRequest;
import com.wms.dto.request.InterWarehouseTransferPutawayAllocationRequest;
import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCheckItemRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCheckRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCountItemRequest;
import com.wms.dto.request.InterWarehouseTransferReceiveCountRequest;
import com.wms.dto.request.InterWarehouseTransferTripAssignRequest;
import com.wms.dto.request.InterWarehouseTransferUpdateRequest;
import com.wms.dto.request.InterWarehouseTransferRejectRequest;
import com.wms.dto.request.TransferReturnRequest;
import com.wms.dto.request.LoadHandoverRequest;
import com.wms.dto.request.OutboundQcRequest;
import com.wms.dto.request.SourceLoadReportItemRequest;
import com.wms.dto.request.SourceLoadReportRequest;
import com.wms.dto.request.AccountingPeriodCloseRequest;
import com.wms.dto.response.AccountingPeriodResponse;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.warehouse_transfer.InterWarehouseTransfer;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferAllocation;
import com.wms.entity.warehouse_transfer.InterWarehouseTransferItem;
import com.wms.entity.order_fulfillment.Trip;
import com.wms.entity.access_control.User;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.warehouse_transfer.InterWarehouseTransferStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.enums.warehouse_location.LocationType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.driver_management.DriverRepository;
import com.wms.repository.stock_receiving.QuarantineRecordRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.InterWarehouseTransferAllocationRepository;
import com.wms.repository.InterWarehouseTransferItemRepository;
import com.wms.repository.InterWarehouseTransferRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.warehouse_transfer.impl.*;
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
    private TrackingAuditUtil auditUtil;
    private EntityManager entityManager;
    private AccountingPeriodService accountingPeriodService;
    private InterWarehouseTransferServiceImpl service;

    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;
    private Warehouse transitWarehouse;
    private WarehouseLocation sourceLocation;
    private WarehouseLocation transitLocation;
    private WarehouseLocation destinationLocation;
    private WarehouseLocation destinationLocation2;
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
    private Inventory destinationInventory2;
    private Inventory quarantineInventory;
    private QuarantineRecord savedQuarantineRecord;
    private Trip transferTrip;
    private final Map<Long, List<Long>> assignments = new HashMap<>();
    private final TrackingAllocationRepository allocationState = new TrackingAllocationRepository();
    private boolean vehicleScheduleOverlap;
    private boolean driverScheduleOverlap;
    private boolean transitWarehouseConfigured;
    private boolean transitLocationConfigured;
    private boolean quarantineLocationConfigured;

    @BeforeEach
    void setUp() {
        sourceWarehouse = warehouse(1L, "HP-01");
        destinationWarehouse = warehouse(2L, "HN-01");
        transitWarehouse = warehouse(99L, "IN_TRANSIT");
        sourceLocation = location(10L, sourceWarehouse, "HP-01-B01", false);
        transitLocation = location(11L, transitWarehouse, "INT-01", false);
        destinationLocation = location(12L, destinationWarehouse, "HN-01-B01", false);
        destinationLocation2 = location(14L, destinationWarehouse, "HN-01-B02", false);
        quarantineLocation = location(13L, destinationWarehouse, "HN-01-Q01", true);
        product = product(21L, "SKU-001", "Nồi inox");
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
        destinationInventory2 = null;
        quarantineInventory = null;
        transferTrip = null;
        vehicleScheduleOverlap = false;
        driverScheduleOverlap = false;
        transitWarehouseConfigured = true;
        transitLocationConfigured = true;
        quarantineLocationConfigured = true;

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
        auditUtil = new TrackingAuditUtil();
        entityManager = proxy(EntityManager.class, new EntityManagerHandler());
        accountingPeriodService = new AccountingPeriodService() {
            @Override
            public List<AccountingPeriodResponse> getAllPeriods(User actor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AccountingPeriodResponse closePeriod(Long id, AccountingPeriodCloseRequest request, User actor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void validateDateInOpenPeriod(LocalDate date) {
                // no-op: transfer document dates in this test are always treated as open
            }

            @Override
            public AccountingPeriod resolveOpenPeriod(LocalDate date) {
                return AccountingPeriod.builder().id(1L).periodName("2026-07").status(null).build();
            }
        };

        InterWarehouseTransferMapper mapper = new InterWarehouseTransferMapper();

        InterWarehouseTransferHelper helper = new InterWarehouseTransferHelper(
                transferRepository, transferItemRepository, allocationRepository,
                inventoryRepository, locationRepository, warehouseRepository, assignmentRepository,
                tripRepository, mapper, auditUtil, entityManager);

        InterWarehouseTransferPlanningService planningService = new InterWarehouseTransferPlanningService(
                transferRepository, transferItemRepository, helper, accountingPeriodService);

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
                discrepancyIncidentRepository, discrepancyHoldEntryRepository);

        service = new InterWarehouseTransferServiceImpl(
                transferRepository, helper, planningService,
                approvalService, shippingService, receivingService);
    }

    private void recordPassingOutboundQcAndHandover() {
        service.recordSourceLoadReport(1L, new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(transferItem.getId(), transferItem.getPlannedQty())), null), sourceManager);
        service.recordOutboundQc(1L, new OutboundQcRequest(true, "QC passed", "outbound-qc.jpg"), sourceManager);
        service.loadHandover(1L, new LoadHandoverRequest("load-handover.jpg"), sourceManager);
    }

    private void moveTransferToCheckedReceiving() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);
        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        null,
                        "Check ok",
                        null)),
                "transfer/receive-qc/1.jpg"),
                destinationStorekeeper);
    }

    @Test
    void plannerLifecycle_createUpdateCancelNewWorks() {
        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "CTM-20260617-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
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
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "manual instruction updated",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("6.00"))));
        InterWarehouseTransferResponse updated = service.updateTransfer(1L, updateRequest, planner);
        assertThat(updated.plannedDate()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(updated.items()).hasSize(1);
        assertThat(updated.items().get(0).plannedQty()).isEqualByComparingTo("6.00");

        InterWarehouseTransferResponse cancelled = service.cancelTransfer(1L,
                new InterWarehouseTransferReasonRequest("Planner cancel"), planner);
        assertThat(cancelled.status()).isEqualTo(InterWarehouseTransferStatus.CANCELLED);
    }

    @Test
    void createTransfer_documentDateInPastFails() {
        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "CTM-PAST-DOC-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                "past document date",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), sourceLocation.getId(),
                        destinationLocation.getId(), new BigDecimal("4.00"))));

        assertThatThrownBy(() -> service.createTransfer(createRequest, planner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DOCUMENT_DATE_MUST_NOT_BE_PAST");
    }

    @Test
    void plannerLifecycle_outsideWarehouseScope_createUpdateCancelFails() {
        User unassignedPlanner = user(999L, UserRole.PLANNER);
        assignments.put(unassignedPlanner.getId(), List.of());

        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "CTM-OUTSIDE-01",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
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
                LocalDate.now(),
                LocalDate.now().plusDays(2),
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
    void approvedRequestConversion_allowsPlannerScopedToDestinationWarehouse() {
        User destinationPlanner = user(998L, UserRole.PLANNER);
        assignments.put(destinationPlanner.getId(), List.of(destinationWarehouse.getId()));

        InterWarehouseTransferCreateRequest createRequest = new InterWarehouseTransferCreateRequest(
                "TRQ-DESTINATION-PLANNER",
                sourceWarehouse.getId(),
                destinationWarehouse.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "approved destination request",
                List.of(new InterWarehouseTransferItemRequest(product.getId(), null, null, new BigDecimal("4.00"))));

        assertThatThrownBy(() -> service.createTransfer(createRequest, destinationPlanner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_SCOPE_REQUIRED");

        InterWarehouseTransferResponse created = service.createTransferFromApprovedRequest(createRequest,
                destinationPlanner);

        assertThat(created.status()).isEqualTo(InterWarehouseTransferStatus.NEW);
        assertThat(created.externalInstructionCode()).isEqualTo("TRQ-DESTINATION-PLANNER");
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

        assertThatThrownBy(() -> service.shipTransfer(1L, sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_LOAD_REPORT_REQUIRED");

        recordPassingOutboundQcAndHandover();
        InterWarehouseTransferResponse shipped = service.shipTransfer(1L, sourceManager);
        assertThat(shipped.items().get(0).loadedQty()).isEqualByComparingTo("5.00");
        assertThat(shipped.items().get(0).sentQty()).isEqualByComparingTo("5.00");

        InterWarehouseTransferResponse unshipped = service.unshipTransfer(1L, sourceManager);
        assertThat(unshipped.items().get(0).sentQty()).isNull();

        service.recordSourceLoadReport(1L, new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(transferItem.getId(), transferItem.getPlannedQty())), null), sourceManager);
        service.recordOutboundQc(1L, new OutboundQcRequest(true, "QC passed again", "outbound-qc-2.jpg"), sourceManager);
        service.shipTransfer(1L, sourceManager);
        service.loadHandover(1L, new LoadHandoverRequest("load-handover-2.jpg"), sourceManager);
        InterWarehouseTransferResponse departed = service.departTransfer(1L, driverUser);
        assertThat(departed.status()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
        assertThat(sourceInventory.getTotalQty()).isEqualByComparingTo("15.00");
        assertThat(transitInventory).isNotNull();
        assertThat(transitInventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(transfer.getTrip().getStatus()).isEqualTo(TripStatus.IN_TRANSIT);
    }

    @Test
    void approveTransfer_insufficientStockFailsWithoutPartialReservation() {
        sourceInventory.setTotalQty(BigDecimal.ONE);
        sourceInventory.setReservedQty(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.approveTransfer(1L, sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("INSUFFICIENT_AVAILABLE_STOCK");

        assertThat(transfer.getStatus()).isEqualTo(InterWarehouseTransferStatus.NEW);
        assertThat(sourceInventory.getReservedQty()).isZero();
        assertThat(allocationState.saved).isEmpty();
        assertThat(auditUtil.lastAction).isNull();
    }

    @Test
    void sourceFlow_requiresWorkerLoadReportBeforeOutboundQcAndAllowsReworkRetry() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);

        assertThatThrownBy(() -> service.recordOutboundQc(1L,
                new OutboundQcRequest(true, "QC too early", "qc.jpg"), sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_LOAD_REPORT_REQUIRED");

        service.recordSourceLoadReport(1L, new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(transferItem.getId(), transferItem.getPlannedQty())), null), sourceManager);
        InterWarehouseTransferResponse failedQc = service.recordOutboundQc(1L,
                new OutboundQcRequest(false, "Mop meo vo hop", "qc-fail.jpg"), sourceManager);
        assertThat(failedQc.sourceLoadReworkRequired()).isTrue();

        assertThatThrownBy(() -> service.loadHandover(1L, new LoadHandoverRequest("handover.jpg"), sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_LOAD_REWORK_REQUIRED");
        assertThatThrownBy(() -> service.departTransfer(1L, driverUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_LOAD_REWORK_REQUIRED");

        InterWarehouseTransferResponse reloaded = service.recordSourceLoadReport(1L, new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(transferItem.getId(), transferItem.getPlannedQty())), "Da doi hang"), sourceManager);
        assertThat(reloaded.sourceLoadReworkRequired()).isFalse();
        assertThat(reloaded.outboundQcPassed()).isNull();

        service.recordOutboundQc(1L, new OutboundQcRequest(true, "QC passed after rework", "qc-pass.jpg"), sourceManager);
        service.loadHandover(1L, new LoadHandoverRequest("handover.jpg"), sourceManager);
        InterWarehouseTransferResponse shipped = service.shipTransfer(1L, sourceManager);
        assertThat(shipped.items().get(0).sentQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void sourceFlow_rejectsLoadReportWhenLoadedQuantityDiffersFromPlanned() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);

        assertThatThrownBy(() -> service.recordSourceLoadReport(1L, new SourceLoadReportRequest(List.of(
                new SourceLoadReportItemRequest(transferItem.getId(), new BigDecimal("4.00"))), ""), sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_LOAD_QTY_MUST_MATCH_PLAN");
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
    void assignTrip_rejectsTripEndingAfterRequiredArrivalDate() {
        transfer.setPlannedDate(LocalDate.now().plusDays(1));
        service.approveTransfer(1L, sourceManager);

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                        LocalDate.now().plusDays(2).atTime(9, 0),
                        LocalDate.now().plusDays(2).atTime(12, 0)),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TRIP_END_MUST_NOT_BE_AFTER_REQUIRED_DATE");
    }

    @Test
    void assignTrip_cancelsApprovedTransferWhenRequiredArrivalDateExpired() {
        transfer.setPlannedDate(LocalDate.now().minusDays(1));
        service.approveTransfer(1L, sourceManager);
        assertThat(sourceInventory.getReservedQty()).isEqualByComparingTo("5.00");

        InterWarehouseTransferResponse response = service.assignTrip(1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                        VALID_TRIP_START, VALID_TRIP_END),
                dispatcher);

        assertThat(response.status()).isEqualTo(InterWarehouseTransferStatus.CANCELLED);
        assertThat(sourceInventory.getReservedQty()).isZero();
        assertThat(transfer.getRejectionReason()).isEqualTo("TRANSFER_REQUIRED_DATE_EXPIRED");
        assertThat(auditUtil.lastAction).isEqualTo(AuditAction.TRANSFER_CANCEL);
    }

    @Test
    void driverArrive_forcesReturnWhenInTransitMissesRequiredArrivalDate() {
        transfer.setPlannedDate(LocalDate.now().plusDays(1));
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                LocalDate.now().plusDays(1).atTime(9, 0), LocalDate.now().plusDays(1).atTime(12, 0)), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        transfer.setPlannedDate(LocalDate.now().minusDays(1));
        transfer.setDriverArrivedAt(null);
        transfer.setArrivalHandoverAt(null);

        InterWarehouseTransferResponse response = service.driverArrive(1L, driverUser);

        assertThat(response.isReturned()).isTrue();
        assertThat(transfer.getDriverArrivedAt()).isNull();
        assertThat(transfer.getReturnReason()).isEqualTo("TRANSFER_REQUIRED_DATE_EXPIRED");
        assertThat(auditUtil.lastAction).isEqualTo(AuditAction.TRANSFER_RETURN_TO_SOURCE);
    }

    @Test
    void assignTrip_rejectsExpiredDriverLicense() {
        service.approveTransfer(1L, sourceManager);
        driver.setLicenseExpiry(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_LICENSE_EXPIRED");
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
    void assignTrip_reportsVehicleScheduleOverlapSeparatelyFromDriver() {
        service.approveTransfer(1L, sourceManager);
        vehicleScheduleOverlap = true;

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("VEHICLE_SCHEDULE_OVERLAP");
    }

    @Test
    void assignTrip_reportsDriverScheduleOverlapSeparatelyFromVehicle() {
        service.approveTransfer(1L, sourceManager);
        driverScheduleOverlap = true;

        assertThatThrownBy(() -> service.assignTrip(
                1L,
                new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(), VALID_TRIP_START,
                        VALID_TRIP_END),
                dispatcher))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_SCHEDULE_OVERLAP");
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
        recordPassingOutboundQcAndHandover();
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
                                new BigDecimal("4.00"),
                                BigDecimal.ZERO,
                                destinationLocation.getId(),
                                "checker adjusted count",
                                null)),
                        "transfer/receive-qc/1.jpg"),
                destinationStorekeeper);
        assertThat(checked.items().get(0).receivedQty()).isEqualByComparingTo("4.00");
        assertThat(checked.items().get(0).qcPassedQty()).isEqualByComparingTo("4.00");

        InterWarehouseTransferResponse pending = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(
                        "shortage due to missing unit",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("4.00")))))),
                destinationStorekeeper);
        assertThat(pending.status()).isEqualTo(InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        assertThat(destinationInventory).isNull();

        InterWarehouseTransferResponse completed = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("shortage due to missing unit"), destinationManager);
        assertThat(completed.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY);
        assertThat(destinationInventory).isNotNull();
        assertThat(destinationInventory.getTotalQty()).isEqualByComparingTo("4.00");
        assertThat(quarantineInventory).isNull();
    }

    @Test
    void finalReceive_requiresManagerApprovalAfterStorekeeperSubmitsMultiBinPutawayPlan() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);
        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        null,
                        "Check ok",
                        null)),
                "transfer/receive-qc/1.jpg"),
                destinationStorekeeper);

        InterWarehouseTransferResponse pending = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(
                        "",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(
                                        new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation.getId(), new BigDecimal("2.00")),
                                        new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation2.getId(), new BigDecimal("3.00")))))),
                destinationStorekeeper);

        assertThat(pending.status()).isEqualTo(InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        assertThat(destinationInventory).isNull();
        assertThat(destinationInventory2).isNull();

        assertThatThrownBy(() -> service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest(""),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WAREHOUSE_MANAGER_APPROVAL_REQUIRED");

        InterWarehouseTransferResponse completed = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(""), destinationManager);

        assertThat(completed.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED);
        assertThat(destinationInventory).isNotNull();
        assertThat(destinationInventory.getLocation()).isSameAs(destinationLocation);
        assertThat(destinationInventory.getTotalQty()).isEqualByComparingTo("2.00");
        assertThat(destinationInventory2).isNotNull();
        assertThat(destinationInventory2.getLocation()).isSameAs(destinationLocation2);
        assertThat(destinationInventory2.getTotalQty()).isEqualByComparingTo("3.00");
    }

    @Test
    void finalReceive_allowsEmptyPutawayPlanWhenAllReceivedStockFailedQc() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);
        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("5.00"),
                        null,
                        null,
                        "All returned stock damaged")),
                "transfer/receive-qc/all-failed.jpg"),
                destinationStorekeeper);

        InterWarehouseTransferResponse pending = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("", List.of()),
                destinationStorekeeper);
        assertThat(pending.status()).isEqualTo(InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        assertThat(destinationInventory).isNull();
        assertThat(quarantineInventory).isNull();

        InterWarehouseTransferResponse completed = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(""), destinationManager);
        assertThat(completed.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED);
        assertThat(destinationInventory).isNull();
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void finalReceive_rejectsShortPutawayPlanEvenWithReason() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);
        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        null,
                        "Check ok",
                        null)),
                "transfer/receive-qc/1.jpg"),
                destinationStorekeeper);

        assertThatThrownBy(() -> service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(
                        "",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("4.00")))))),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED");

        assertThatThrownBy(() -> service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(
                        "Missing one unit during putaway",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("4.00")))))),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED");
    }

    @Test
    void finalReceive_createsDiscrepancyWhenStorekeeperConfirmsOverReceipt() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(
                        transferItem.getId(), new BigDecimal("7.00"), "received two extra units"))),
                destinationWorker);
        service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("7.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        null)),
                "transfer/receive-qc/over.jpg"),
                destinationStorekeeper);

        InterWarehouseTransferResponse pending = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(
                        "received two extra units",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("5.00")))))),
                destinationStorekeeper);

        assertThat(pending.status()).isEqualTo(InterWarehouseTransferStatus.PUTAWAY_PENDING_APPROVAL);
        assertThat(destinationInventory).isNull();

        InterWarehouseTransferResponse completed = service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest(""), destinationManager);

        assertThat(completed.status()).isEqualTo(InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY);
        assertThat(destinationInventory).isNotNull();
        assertThat(destinationInventory.getTotalQty()).isEqualByComparingTo("5.00");
    }

    @Test
    void finalReceive_rejectsDuplicatePutawayItemAndLocation() {
        moveTransferToCheckedReceiving();

        assertThatThrownBy(() -> service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("",
                        List.of(
                                new InterWarehouseTransferFinalPutawayItemRequest(
                                        transferItem.getId(),
                                        List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation.getId(), new BigDecimal("2.00")))),
                                new InterWarehouseTransferFinalPutawayItemRequest(
                                        transferItem.getId(),
                                        List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation2.getId(), new BigDecimal("3.00")))))),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DUPLICATE_PUTAWAY_ITEM");

        assertThatThrownBy(() -> service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(
                                        new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation.getId(), new BigDecimal("2.00")),
                                        new InterWarehouseTransferPutawayAllocationRequest(
                                                destinationLocation.getId(), new BigDecimal("3.00")))))),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DUPLICATE_PUTAWAY_LOCATION");
    }

    @Test
    void finalReceive_rejectsPutawayQuantityOverQcPassed() {
        moveTransferToCheckedReceiving();

        assertThatThrownBy(() -> service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("6.00")))))),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PUTAWAY_QUANTITY_MUST_MATCH_QC_PASSED");
    }

    @Test
    void finalReceive_rejectsMissingTransitWarehouseOrLocationConfiguration() {
        moveTransferToCheckedReceiving();
        service.finalReceive(1L,
                new InterWarehouseTransferFinalReceiveRequest("",
                        List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                                transferItem.getId(),
                                List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                        destinationLocation.getId(), new BigDecimal("5.00")))))),
                destinationStorekeeper);

        transitWarehouseConfigured = false;
        assertThatThrownBy(() -> service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest(""),
                destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED");

        transitWarehouseConfigured = true;
        transitLocationConfigured = false;
        assertThatThrownBy(() -> service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest(""),
                destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("IN_TRANSIT_LOCATION_NOT_CONFIGURED");
    }

    @Test
    void receiveCount_overReceipt_isAllowedAndRoutedToHold() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
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
    void returnToSource_blocksDirectSourceManagerReturnWhileTruckInTransit() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        transfer.setDriverArrivedAt(null);
        transfer.setArrivalHandoverAt(null);

        TransferReturnRequest req = new TransferReturnRequest("Overdue return");
        assertThatThrownBy(() -> service.returnToSource(1L, req, driverUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");

        assertThatThrownBy(() -> service.returnToSource(1L, req, planner))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");

        assertThatThrownBy(() -> service.returnToSource(1L, req, destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");

        assertThatThrownBy(() -> service.returnToSource(1L, req, user(14L, UserRole.CEO)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");

        assertThatThrownBy(() -> service.returnToSource(1L, req, user(15L, UserRole.ADMIN)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");

        assertThatThrownBy(() -> service.returnToSource(1L, req, sourceManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SOURCE_RETURN_DISABLED");
    }

    @Test
    void quarantineReject_storekeeper_success() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("Storekeeper found completely broken boxes on arrival");

        InterWarehouseTransferResponse response = service.quarantineReject(1L, request, destinationStorekeeper);

        assertThat(response.status()).isEqualTo(InterWarehouseTransferStatus.QUARANTINED);
        assertThat(response.rejectionReason()).isEqualTo("Storekeeper found completely broken boxes on arrival");
        assertThat(quarantineInventory).isNotNull();
        assertThat(quarantineInventory.getTotalQty()).isEqualByComparingTo("5.00");
        assertThat(savedQuarantineRecord.getReason()).isEqualTo("Storekeeper found completely broken boxes on arrival");
    }

    @Test
    void quarantineReject_requiresArrivalHandoverAndWorkerCount() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("Broken before receiving");
        transfer.setDriverArrivedAt(null);
        transfer.setArrivalHandoverAt(null);

        assertThatThrownBy(() -> service.quarantineReject(1L, request, destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("DRIVER_ARRIVE_REQUIRED");

        transfer.setDriverArrivedAt(OffsetDateTime.now());

        assertThatThrownBy(() -> service.quarantineReject(1L, request, destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ARRIVAL_HANDOVER_REQUIRED");

        transfer.setArrivalHandoverAt(OffsetDateTime.now());

        assertThatThrownBy(() -> service.quarantineReject(1L, request, destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WORKER_COUNT_REQUIRED");
    }

    @Test
    void quarantineReject_manager_success() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
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
                        null)),
                "transfer/receive-qc/1.jpg"),
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
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);

        InterWarehouseTransferRejectRequest request = new InterWarehouseTransferRejectRequest();
        request.setRejectionReason("");

        assertThatThrownBy(() -> service.quarantineReject(1L, request, destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("REJECTION_REASON_REQUIRED");
    }

    @Test
    void receiveCheck_rejectsQcFailureWhenQuarantineBinMissing() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker);

        quarantineLocationConfigured = false;

        assertThatThrownBy(() -> service.receiveCheck(1L, new InterWarehouseTransferReceiveCheckRequest(List.of(
                new InterWarehouseTransferReceiveCheckItemRequest(
                        transferItem.getId(),
                        new BigDecimal("5.00"),
                        new BigDecimal("4.00"),
                        BigDecimal.ONE,
                        destinationLocation.getId(),
                        "Check ok",
                        "Broken box")),
                "transfer/receive-qc/1.jpg"),
                destinationStorekeeper))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("QUARANTINE_LOCATION_NOT_CONFIGURED");
    }

    @Test
    void receiving_blocksIfArriveOrHandoverMissing() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
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
    void getTransferById_reportsOverdueWithoutMutatingTransferOrTrip() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        transfer.getTrip().setPlannedEndAt(LocalDateTime.now().minusHours(1));

        InterWarehouseTransferResponse response = service.getTransferById(1L, destinationWorker);

        assertThat(response.tripOverdue()).isTrue();
        assertThat(transfer.getStatus()).isEqualTo(InterWarehouseTransferStatus.IN_TRANSIT);
        assertThat(transfer.getTrip().getStatus()).isEqualTo(TripStatus.IN_TRANSIT);
        assertThat(transfer.getRejectionReason()).isNull();
        assertThat(transferItem.getSentQty()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void receiving_blocksWhenTripIsOverdueBeforeReturnDecision() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
        service.shipTransfer(1L, sourceManager);
        service.departTransfer(1L, driverUser);
        transfer.getTrip().setPlannedEndAt(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> service.receiveCount(1L, new InterWarehouseTransferReceiveCountRequest(List.of(
                new InterWarehouseTransferReceiveCountItemRequest(transferItem.getId(), new BigDecimal("5.00"), null))),
                destinationWorker))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("TRANSFER_TRIP_OVERDUE");
    }

    @Test
    void receiving_blocksIfBinCapacityExceeded() {
        service.approveTransfer(1L, sourceManager);
        service.assignTrip(1L, new InterWarehouseTransferTripAssignRequest(vehicle.getId(), driver.getId(),
                VALID_TRIP_START, VALID_TRIP_END), dispatcher);
        recordPassingOutboundQcAndHandover();
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
                        null)),
                "transfer/receive-qc/1.jpg"),
                destinationStorekeeper);

        service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest(
                "",
                List.of(new InterWarehouseTransferFinalPutawayItemRequest(
                        transferItem.getId(),
                        List.of(new InterWarehouseTransferPutawayAllocationRequest(
                                destinationLocation.getId(), new BigDecimal("5.00")))))),
                destinationStorekeeper);

        // Manager approval should throw BIN_CAPACITY_EXCEEDED because 5.00 * 1.00 > 0.01
        assertThatThrownBy(() -> service.finalReceive(1L, new InterWarehouseTransferFinalReceiveRequest("final"), destinationManager))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("BIN_CAPACITY_EXCEEDED");
    }

    private InterWarehouseTransfer transfer() {
        InterWarehouseTransfer value = new InterWarehouseTransfer();
        value.setId(1L);
        value.setTransferNumber("TRF-20260617-0001");
        value.setExternalInstructionCode("CTM-0001");
        value.setSourceWarehouse(sourceWarehouse);
        value.setDestinationWarehouse(destinationWarehouse);
        value.setStatus(InterWarehouseTransferStatus.NEW);
        value.setOutboundQcPassed(null);
        value.setLoadHandoverPhotoRef(null);
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

    private Product product(Long id, String sku, String name) {
        Product value = new Product();
        value.setId(id);
        value.setSku(sku);
        value.setName(name);
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
            if (destinationInventory2 != null && destinationInventory2.getId().equals(id))
                return destinationInventory2;
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
            if (matches(destinationInventory2, warehouseId, productId, batchId, locationId))
                return destinationInventory2;
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
            } else if (destinationLocation2.getId().equals(value.getLocation().getId())) {
                destinationInventory2 = value;
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
            if ("findFirstByTypeAndIsActiveTrue".equals(method.getName())) {
                return transitWarehouseConfigured ? Optional.of(transitWarehouse) : Optional.empty();
            }
            return defaultValue(method.getReturnType());
        }
    }

    private final class LocationRepoHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findByWarehouseIdAndTypeAndIsActiveTrue" -> transitLocationConfigured ? List.of(transitLocation) : List.of();
                case "findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue" -> quarantineLocationConfigured ? List.of(quarantineLocation) : List.of();
                case "findById" -> {
                    Long id = (Long) args[0];
                    if (destinationLocation.getId().equals(id))
                        yield Optional.of(destinationLocation);
                    if (destinationLocation2.getId().equals(id))
                        yield Optional.of(destinationLocation2);
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
                case "existsVehicleScheduleOverlap", "existsVehicleScheduleOverlapExcludingTrip" -> vehicleScheduleOverlap;
                case "existsDriverScheduleOverlap", "existsDriverScheduleOverlapExcludingTrip" -> driverScheduleOverlap;
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
                    if (destinationLocation2.getId().equals(id))
                        return destinationLocation2;
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
                savedQuarantineRecord = (QuarantineRecord) args[0];
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

}
