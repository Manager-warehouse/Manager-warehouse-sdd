package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.SplitDeliveryLegItemRequest;
import com.wms.dto.request.SplitDeliveryLegRequest;
import com.wms.dto.request.SplitLegFailureRequest;
import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.entity.access_control.User;
import com.wms.entity.driver_management.Driver;
import com.wms.entity.fleet_management.Vehicle;
import com.wms.entity.order_fulfillment.Delivery;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.order_fulfillment.OutboundQcRecord;
import com.wms.entity.order_fulfillment.SplitDeliveryLeg;
import com.wms.entity.order_fulfillment.SplitDeliveryLegItem;
import com.wms.entity.order_fulfillment.SplitDeliveryPlan;
import com.wms.entity.order_fulfillment.Trip;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_control.Batch;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryStatus;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.OutboundQcRecordRepository;
import com.wms.repository.SplitDeliveryLegItemRepository;
import com.wms.repository.SplitDeliveryLegRepository;
import com.wms.repository.SplitDeliveryPlanRepository;
import com.wms.repository.TripDeliveryOrderRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.repository.driver_management.DriverRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.order_fulfillment.impl.SplitDeliveryPlanServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SplitDeliveryPlanServiceImplTest {

    @Mock private SplitDeliveryPlanRepository splitPlanRepository;
    @Mock private SplitDeliveryLegRepository splitLegRepository;
    @Mock private SplitDeliveryLegItemRepository splitLegItemRepository;
    @Mock private TripRepository tripRepository;
    @Mock private TripDeliveryOrderRepository tripDeliveryOrderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private WarehouseLocationRepository warehouseLocationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OutboundQcRecordRepository outboundQcRecordRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private AuditLogService auditLogService;

    private SplitDeliveryPlanServiceImpl service;
    private User dispatcher;
    private Warehouse warehouse;
    private DeliveryOrder order;
    private DeliveryOrderItem item;
    private Product product;
    private Batch batch;
    private Driver driver1;
    private Driver driver2;
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private List<SplitDeliveryLeg> savedLegs;
    private List<SplitDeliveryLegItem> savedLegItems;
    private long legSequence;

    @BeforeEach
    void setUp() {
        service = new SplitDeliveryPlanServiceImpl(splitPlanRepository, splitLegRepository, splitLegItemRepository,
                tripRepository, tripDeliveryOrderRepository, deliveryRepository, deliveryOrderRepository,
                deliveryOrderItemRepository, vehicleRepository, driverRepository, warehouseRepository,
                warehouseLocationRepository, inventoryRepository, outboundQcRecordRepository, assignmentRepository,
                auditLogService);
        dispatcher = User.builder().id(1L).role(UserRole.DISPATCHER).fullName("Dispatcher").build();
        warehouse = Warehouse.builder().id(20L).code("HP").name("Hai Phong").type(WarehouseType.PHYSICAL)
                .isActive(true).build();
        product = Product.builder().id(501L).sku("P-1").name("Pan").unit("pcs")
                .weightKg(new BigDecimal("1.00")).volumeM3(new BigDecimal("0.10")).isActive(true).build();
        batch = Batch.builder().id(601L).batchNumber("B-1").batchCode("B-1").product(product).warehouse(warehouse)
                .receivedDate(LocalDate.now()).quantity(new BigDecimal("100.00")).build();
        order = DeliveryOrder.builder().id(100L).doNumber("DO-100").warehouse(warehouse)
                .status(DeliveryOrderStatus.WAREHOUSE_APPROVED).build();
        item = DeliveryOrderItem.builder().id(401L).deliveryOrder(order).product(product).batch(batch)
                .requestedQty(new BigDecimal("100.00")).qcPassQty(new BigDecimal("100.00"))
                .issuedQty(BigDecimal.ZERO).build();
        driver1 = driver(301L, 11L);
        driver2 = driver(302L, 12L);
        vehicle1 = vehicle(201L, warehouse, VehicleStatus.AVAILABLE, "100.00", "20.00");
        vehicle2 = vehicle(202L, warehouse, VehicleStatus.AVAILABLE, "100.00", "20.00");
        savedLegs = new ArrayList<>();
        savedLegItems = new ArrayList<>();
        legSequence = 1L;
        baseCreateStubs();
    }

    @Test
    void createPlan_acceptsFullAllocationAcrossMultipleVehicles() {
        var response = service.createPlan(createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 301L),
                dispatcher);

        assertThat(response.getStatus()).isEqualTo(SplitDeliveryPlanStatus.PLANNED);
        assertThat(response.getTotalDriverCount()).isEqualTo(2);
        assertThat(savedLegs).hasSize(2);
        verify(auditLogService).log(eq(dispatcher), any(), eq("SPLIT_DELIVERY_PLAN"), anyLong(), any(), eq(20L),
                any(), any());
    }

    @Test
    void createPlan_acceptsAndPersistsAllocationAcrossMultipleQcPassedBatches() {
        item.setBatch(null);
        Batch secondBatch = Batch.builder().id(602L).batchNumber("B-2").batchCode("B-2").product(product)
                .warehouse(warehouse).receivedDate(LocalDate.now()).quantity(new BigDecimal("40.00")).build();
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(100L)))
                .thenReturn(List.of(qcRecord(batch, new BigDecimal("60.00")),
                        qcRecord(secondBatch, new BigDecimal("40.00"))));
        SplitDeliveryPlanCreateRequest request = createRequest(
                new BigDecimal("60.00"), new BigDecimal("40.00"), 301L);
        request.getLegs().get(1).getItems().get(0).setBatchId(602L);

        service.createPlan(request, dispatcher);

        assertThat(savedLegItems).extracting(legItem -> legItem.getBatch().getId())
                .containsExactlyInAnyOrder(601L, 602L);
    }

    @Test
    void createPlan_rejectsBatchWithoutQcPassedQuantity() {
        item.setBatch(null);
        SplitDeliveryPlanCreateRequest request = createRequest(
                new BigDecimal("60.00"), new BigDecimal("40.00"), 301L);
        request.getLegs().get(1).getItems().get(0).setBatchId(999L);

        assertThatThrownBy(() -> service.createPlan(request, dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_DELIVERY_PLAN_INVALID");
    }

    @Test
    void createPlan_rejectsUnderOrOverAllocationAndInvalidLeadDriver() {
        assertThatThrownBy(() -> service.createPlan(
                createRequest(new BigDecimal("50.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_DELIVERY_INCOMPLETE");

        assertThatThrownBy(() -> service.createPlan(
                createRequest(new BigDecimal("70.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_DELIVERY_INCOMPLETE");

        assertThatThrownBy(() -> service.createPlan(
                createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 999L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_LEAD_DRIVER_REQUIRED");
    }

    @Test
    void createPlan_rejectsUnavailableWrongWarehouseActiveTripOrOverCapacityVehicle() {
        vehicle1.setStatus(VehicleStatus.MAINTENANCE);
        assertThatThrownBy(() -> service.createPlan(createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_VEHICLE_NOT_AVAILABLE");
        vehicle1.setStatus(VehicleStatus.AVAILABLE);

        vehicle1.setWarehouse(Warehouse.builder().id(99L).code("HN").isActive(true).build());
        assertThatThrownBy(() -> service.createPlan(createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_VEHICLE_NOT_AVAILABLE");
        vehicle1.setWarehouse(warehouse);

        when(tripRepository.existsActiveVehicleAssignment(eq(201L), any(), eq(null))).thenReturn(true);
        assertThatThrownBy(() -> service.createPlan(createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_VEHICLE_NOT_AVAILABLE");
        when(tripRepository.existsActiveVehicleAssignment(eq(201L), any(), eq(null))).thenReturn(false);

        vehicle1.setMaxWeightKg(new BigDecimal("50.00"));
        assertThatThrownBy(() -> service.createPlan(createRequest(new BigDecimal("60.00"), new BigDecimal("40.00"), 301L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_LEG_OVER_CAPACITY");
    }

    @Test
    void updatePlan_replacesVehicleBeforeDeparture() {
        SplitDeliveryPlan plan = existingPlan();
        List<SplitDeliveryLeg> existingLegs = existingLegs(plan);
        Vehicle replacement = vehicle(203L, warehouse, VehicleStatus.AVAILABLE, "100.00", "20.00");
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L))
                .thenReturn(existingLegs, existingLegs, savedLegs);
        when(vehicleRepository.findWithWarehouseById(203L)).thenReturn(Optional.of(replacement));

        SplitDeliveryPlanUpdateRequest request = updateRequest(203L);
        var response = service.updatePlan(900L, request, dispatcher);

        assertThat(response.getLegs()).hasSize(2);
        assertThat(savedLegs.get(0).getVehicle().getId()).isEqualTo(203L);
        verify(tripDeliveryOrderRepository).deactivateByTripId(501L);
    }

    @Test
    void confirmDriverReadiness_keepsPlanPlannedUntilEveryDriverIsReady() {
        SplitDeliveryPlan plan = existingPlan();
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findBySplitPlanIdAndDriverUserId(900L, 11L)).thenReturn(Optional.of(legs.get(0)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);

        var response = service.confirmDriverReadiness(900L, driver1.getUser());

        assertThat(response.getStatus()).isEqualTo(SplitDeliveryPlanStatus.PLANNED);
        assertThat(response.getReadyDriverCount()).isEqualTo(1);
        verify(deliveryOrderRepository, never()).save(any());
    }

    @Test
    void departPlan_movesAllReadyLegsAndStagingStockWhenLeadDriverConfirms() {
        SplitDeliveryPlan plan = existingPlan();
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        legs.get(0).setReadinessConfirmedAt(OffsetDateTime.now());
        List<SplitDeliveryLegItem> legItems = List.of(legItem(legs.get(0), new BigDecimal("60.00")),
                legItem(legs.get(1), new BigDecimal("40.00")));
        Warehouse transitWarehouse = Warehouse.builder().id(99L).code("TRANSIT").type(WarehouseType.IN_TRANSIT)
                .isActive(true).build();
        WarehouseLocation staging = WarehouseLocation.builder().id(701L).warehouse(warehouse).code("STAGE")
                .isActive(true).build();
        WarehouseLocation transitLocation = WarehouseLocation.builder().id(702L).warehouse(transitWarehouse)
                .code("TRANSIT-LOC").isActive(true).build();
        Inventory stagingInventory = Inventory.builder().id(801L).warehouse(warehouse).product(product).batch(batch)
                .location(staging).totalQty(new BigDecimal("100.00")).reservedQty(new BigDecimal("100.00"))
                .costPrice(BigDecimal.TEN).build();

        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findBySplitPlanIdAndDriverUserId(900L, 12L)).thenReturn(Optional.of(legs.get(1)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);
        when(splitLegItemRepository.findBySplitLegIdIn(List.of(1L, 2L))).thenReturn(legItems);
        when(warehouseRepository.findFirstByTypeAndIsActiveTrue(WarehouseType.IN_TRANSIT)).thenReturn(Optional.of(transitWarehouse));
        when(warehouseLocationRepository.findFirstByWarehouseIdAndIsActiveTrue(99L)).thenReturn(Optional.of(transitLocation));
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(100L)))
                .thenReturn(List.of(qcRecord(staging)));
        when(inventoryRepository.findConcreteRowForTripMovement(eq(20L), eq(501L), eq(601L), eq(701L)))
                .thenReturn(Optional.of(stagingInventory));
        when(inventoryRepository.findConcreteRowForTripMovement(eq(99L), eq(501L), eq(601L), eq(702L)))
                .thenReturn(Optional.empty());
        when(deliveryRepository.findMaxAttemptNumberByDeliveryOrderId(100L)).thenReturn(0);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var readiness = service.confirmDriverReadiness(900L, driver2.getUser());
        assertThat(readiness.getStatus()).isEqualTo(SplitDeliveryPlanStatus.PLANNED);

        var response = service.departPlan(900L, driver1.getUser());
        assertThat(response.getStatus()).isEqualTo(SplitDeliveryPlanStatus.IN_TRANSIT);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.IN_TRANSIT);
        assertThat(stagingInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(item.getIssuedQty()).isEqualByComparingTo("100.00");
    }

    @Test
    void confirmDealerArrival_recordsOnlyTheAssignedDriverLeg() {
        SplitDeliveryPlan plan = existingPlan();
        plan.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        legs.forEach(leg -> leg.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT));
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findById(1L)).thenReturn(Optional.of(legs.get(0)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);

        var response = service.confirmDealerArrival(900L, 1L, driver1.getUser());

        assertThat(response.getDealerArrivedAt()).isNotNull();
        assertThat(legs.get(0).getDealerArrivedAt()).isNotNull();
        assertThat(legs.get(1).getDealerArrivedAt()).isNull();
        verify(splitLegRepository).save(legs.get(0));
    }

    @Test
    void confirmHandover_waitsUntilEverySplitVehicleArrives() {
        SplitDeliveryPlan plan = existingPlan();
        plan.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        legs.forEach(leg -> leg.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT));
        legs.get(0).setDealerArrivedAt(OffsetDateTime.now());
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findById(1L)).thenReturn(Optional.of(legs.get(0)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);

        assertThatThrownBy(() -> service.confirmHandover(900L, 1L, driver1.getUser()))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code").isEqualTo("SPLIT_DELIVERY_INCOMPLETE");

        assertThat(legs.get(0).getHandoverConfirmedAt()).isNull();
    }

    @Test
    void confirmHandover_enablesLeadPodOtpAfterEveryLegHandsOver() {
        SplitDeliveryPlan plan = existingPlan();
        plan.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        legs.forEach(leg -> {
            leg.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
            leg.setDealerArrivedAt(OffsetDateTime.now());
        });
        legs.get(1).setHandoverConfirmedAt(OffsetDateTime.now());
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findById(1L)).thenReturn(Optional.of(legs.get(0)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);

        var response = service.confirmHandover(900L, 1L, driver1.getUser());

        assertThat(response.isAllLegsArrived()).isTrue();
        assertThat(response.isAllLegsHandedOver()).isTrue();
        assertThat(response.isLeadPodOtpEnabled()).isTrue();
    }

    @Test
    void failDeliveryLeg_returnsTheWholeDeliveryOrderAndEveryLeg() {
        SplitDeliveryPlan plan = existingPlan();
        plan.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
        order.setStatus(DeliveryOrderStatus.IN_TRANSIT);
        List<SplitDeliveryLeg> legs = existingLegs(plan);
        legs.forEach(leg -> leg.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT));
        Delivery currentAttempt = Delivery.builder().id(800L).deliveryOrder(order)
                .status(DeliveryStatus.IN_TRANSIT).build();
        SplitLegFailureRequest request = new SplitLegFailureRequest();
        request.setFailureReason("One vehicle cannot complete handover");
        when(splitPlanRepository.findDetailedById(900L)).thenReturn(Optional.of(plan));
        when(splitLegRepository.findById(1L)).thenReturn(Optional.of(legs.get(0)));
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenReturn(legs);
        when(deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(eq(100L), any()))
                .thenReturn(Optional.of(currentAttempt));

        service.failDeliveryLeg(900L, 1L, request, driver1.getUser());

        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.RETURNED);
        assertThat(currentAttempt.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(plan.getStatus()).isEqualTo(SplitDeliveryPlanStatus.RETURNED);
        assertThat(legs).allMatch(leg -> leg.getStatus() == SplitDeliveryPlanStatus.RETURNED);
    }

    private void baseCreateStubs() {
        when(deliveryOrderRepository.findWithDealerAndWarehouseById(100L)).thenReturn(Optional.of(order));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(100L)).thenReturn(List.of(item));
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(100L)))
                .thenReturn(List.of(qcRecord(batch, new BigDecimal("100.00"))));
        when(vehicleRepository.findWithWarehouseById(201L)).thenReturn(Optional.of(vehicle1));
        when(vehicleRepository.findWithWarehouseById(202L)).thenReturn(Optional.of(vehicle2));
        when(driverRepository.findWithWarehouseAndUserById(301L)).thenReturn(Optional.of(driver1));
        when(driverRepository.findWithWarehouseAndUserById(302L)).thenReturn(Optional.of(driver2));
        when(splitPlanRepository.save(any(SplitDeliveryPlan.class))).thenAnswer(invocation -> {
            SplitDeliveryPlan plan = invocation.getArgument(0);
            if (plan.getId() == null) {
                plan.setId(900L);
            }
            return plan;
        });
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            if (trip.getId() == null) {
                trip.setId(500L + legSequence);
            }
            return trip;
        });
        when(splitLegRepository.save(any(SplitDeliveryLeg.class))).thenAnswer(invocation -> {
            SplitDeliveryLeg leg = invocation.getArgument(0);
            if (leg.getId() == null) {
                leg.setId(legSequence++);
            }
            savedLegs.add(leg);
            return leg;
        });
        when(splitLegItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<SplitDeliveryLegItem> entities = invocation.getArgument(0);
            savedLegItems.addAll(entities);
            return entities;
        });
        when(splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(900L)).thenAnswer(invocation -> savedLegs);
    }

    private SplitDeliveryPlanCreateRequest createRequest(BigDecimal firstQty, BigDecimal secondQty, Long leadDriverId) {
        SplitDeliveryPlanCreateRequest request = new SplitDeliveryPlanCreateRequest();
        request.setDoId(100L);
        request.setLeadDriverId(leadDriverId);
        request.setPlannedStartAt(LocalDateTime.now().plusHours(1));
        request.setPlannedEndAt(LocalDateTime.now().plusHours(3));
        request.setLegs(List.of(legRequest(201L, 301L, 1, firstQty), legRequest(202L, 302L, 2, secondQty)));
        return request;
    }

    private SplitDeliveryPlanUpdateRequest updateRequest(Long firstVehicleId) {
        SplitDeliveryPlanUpdateRequest request = new SplitDeliveryPlanUpdateRequest();
        request.setLeadDriverId(301L);
        request.setPlannedStartAt(LocalDateTime.now().plusHours(1));
        request.setPlannedEndAt(LocalDateTime.now().plusHours(3));
        request.setLegs(List.of(legRequest(firstVehicleId, 301L, 1, new BigDecimal("60.00")),
                legRequest(202L, 302L, 2, new BigDecimal("40.00"))));
        return request;
    }

    private SplitDeliveryLegRequest legRequest(Long vehicleId, Long driverId, int stopOrder, BigDecimal quantity) {
        SplitDeliveryLegRequest leg = new SplitDeliveryLegRequest();
        leg.setVehicleId(vehicleId);
        leg.setDriverId(driverId);
        leg.setStopOrder(stopOrder);
        SplitDeliveryLegItemRequest itemRequest = new SplitDeliveryLegItemRequest();
        itemRequest.setDoItemId(401L);
        itemRequest.setProductId(501L);
        itemRequest.setBatchId(601L);
        itemRequest.setQuantity(quantity);
        leg.setItems(List.of(itemRequest));
        return leg;
    }

    private SplitDeliveryPlan existingPlan() {
        return SplitDeliveryPlan.builder().id(900L).planNumber("SDP-1").deliveryOrder(order).warehouse(warehouse)
                .dispatcher(dispatcher).leadDriver(driver1).status(SplitDeliveryPlanStatus.PLANNED)
                .plannedStartAt(LocalDateTime.now().plusHours(1)).createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()).build();
    }

    private List<SplitDeliveryLeg> existingLegs(SplitDeliveryPlan plan) {
        Trip trip1 = Trip.builder().id(501L).tripNumber("TRIP-1").warehouse(warehouse).vehicle(vehicle1)
                .driver(driver1).status(TripStatus.PLANNED).plannedEndAt(LocalDateTime.now().plusHours(3)).build();
        Trip trip2 = Trip.builder().id(502L).tripNumber("TRIP-2").warehouse(warehouse).vehicle(vehicle2)
                .driver(driver2).status(TripStatus.PLANNED).plannedEndAt(LocalDateTime.now().plusHours(3)).build();
        return List.of(SplitDeliveryLeg.builder().id(1L).splitPlan(plan).trip(trip1).vehicle(vehicle1).driver(driver1)
                        .stopOrder(1).status(SplitDeliveryPlanStatus.PLANNED).build(),
                SplitDeliveryLeg.builder().id(2L).splitPlan(plan).trip(trip2).vehicle(vehicle2).driver(driver2)
                        .stopOrder(2).status(SplitDeliveryPlanStatus.PLANNED).build());
    }

    private SplitDeliveryLegItem legItem(SplitDeliveryLeg leg, BigDecimal quantity) {
        return SplitDeliveryLegItem.builder().splitLeg(leg).deliveryOrderItem(item).product(product).batch(batch)
                .quantity(quantity).build();
    }

    private OutboundQcRecord qcRecord(WarehouseLocation staging) {
        OutboundQcRecord record = qcRecord(batch, new BigDecimal("100.00"));
        record.setStagingLocation(staging);
        return record;
    }

    private OutboundQcRecord qcRecord(Batch sourceBatch, BigDecimal qcPassQty) {
        OutboundQcRecord record = new OutboundQcRecord();
        record.setDeliveryOrder(order);
        record.setDeliveryOrderItem(item);
        record.setBatch(sourceBatch);
        record.setQcPassQty(qcPassQty);
        return record;
    }

    private Driver driver(Long id, Long userId) {
        return Driver.builder().id(id).user(User.builder().id(userId).role(UserRole.DRIVER).build())
                .warehouse(warehouse).status(DriverStatus.AVAILABLE).licenseExpiry(LocalDate.now().plusYears(1))
                .isActive(true).build();
    }

    private Vehicle vehicle(Long id, Warehouse vehicleWarehouse, VehicleStatus status, String maxWeight, String maxVolume) {
        return Vehicle.builder().id(id).warehouse(vehicleWarehouse).plateNumber("29C-" + id).status(status)
                .maxWeightKg(new BigDecimal(maxWeight)).maxVolumeM3(new BigDecimal(maxVolume)).isActive(true).build();
    }
}
