package com.wms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.request.TripCancelRequest;
import com.wms.dto.request.TripCreateRequest;
import com.wms.dto.request.TripDeliveryOrderRequest;
import com.wms.dto.request.TripUpdateRequest;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.Driver;
import com.wms.entity.Batch;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundQcRecord;
import com.wms.entity.Product;
import com.wms.entity.Trip;
import com.wms.entity.TripDeliveryOrder;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DriverStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.UserRole;
import com.wms.enums.VehicleStatus;
import com.wms.exception.OutboundDeliveryException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DeliveryOrderRepository;
import com.wms.repository.DeliveryRepository;
import com.wms.repository.DriverRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.OutboundQcRecordRepository;
import com.wms.repository.TripDeliveryOrderRepository;
import com.wms.repository.TripRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.VehicleRepository;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.WarehouseRepository;
import com.wms.service.impl.TripServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

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

    private TripServiceImpl service;
    private User dispatcher;
    private Warehouse warehouse;
    private Vehicle vehicle;
    private Driver driver;
    private User driverUser;
    private DeliveryOrder order;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new TripServiceImpl(tripRepository, tripDeliveryOrderRepository, deliveryRepository,
                deliveryOrderRepository, deliveryOrderItemRepository, vehicleRepository, driverRepository,
                warehouseRepository, warehouseLocationRepository, inventoryRepository, outboundQcRecordRepository,
                assignmentRepository, auditLogService);
        dispatcher = user(1L, UserRole.DISPATCHER);
        warehouse = warehouse(20L);
        vehicle = vehicle(301L, warehouse, VehicleStatus.AVAILABLE);
        driverUser = user(2L, UserRole.DRIVER);
        driver = driver(401L, warehouse, DriverStatus.AVAILABLE, driverUser);
        order = order(101L, warehouse, DeliveryOrderStatus.WAREHOUSE_APPROVED);
        product = product(30L);
    }

    @Test
    void listTrips_filtersByAssignedWarehouseAndStatus() {
        Trip trip = plannedTrip();
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(tripRepository.findByWarehouseIdInAndOptionalStatus(List.of(20L), TripStatus.PLANNED))
                .thenReturn(List.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        var responses = service.listTrips(null, TripStatus.PLANNED, dispatcher);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTripNumber()).isEqualTo("TRIP-20260620-0001");
        assertThat(responses.get(0).getDeliveryOrders()).hasSize(1);
    }

    @Test
    void listTrips_rejectsUnassignedWarehouseScope() {
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));

        assertThatThrownBy(() -> service.listTrips(99L, null, dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
        verify(tripRepository, never()).findByWarehouseIdInAndOptionalStatus(any(), any());
    }

    @Test
    void listTrips_adminWithoutWarehouseId_retrievesAllActiveWarehouses() {
        User admin = user(3L, UserRole.ADMIN);
        Trip trip = plannedTrip();
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(warehouse));
        when(tripRepository.findByWarehouseIdInAndOptionalStatus(List.of(20L), TripStatus.PLANNED))
                .thenReturn(List.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        var responses = service.listTrips(null, TripStatus.PLANNED, admin);

        assertThat(responses).hasSize(1);
        verify(warehouseRepository).findByIsActive(true);
        verify(assignmentRepository, never()).findWarehouseIdsByUserId(anyLong());
    }

    @Test
    void listTrips_adminWithWarehouseId_bypassesWarehouseScopeCheck() {
        User admin = user(3L, UserRole.ADMIN);
        Trip trip = plannedTrip();
        when(tripRepository.findByWarehouseIdInAndOptionalStatus(List.of(20L), TripStatus.PLANNED))
                .thenReturn(List.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        var responses = service.listTrips(20L, TripStatus.PLANNED, admin);

        assertThat(responses).hasSize(1);
        verify(assignmentRepository, never()).findWarehouseIdsByUserId(anyLong());
    }

    @Test
    void createTrip_successKeepsDeliveryOrderWarehouseApproved() {
        stubCreateHappyPath();

        var response = service.createTrip(createRequest(101L), dispatcher);

        assertThat(response.getStatus()).isEqualTo(TripStatus.PLANNED);
        assertThat(response.getDeliveryOrders()).hasSize(1);
        assertThat(order.getStatus()).isEqualTo(DeliveryOrderStatus.WAREHOUSE_APPROVED);
        verify(tripDeliveryOrderRepository).saveAll(any());
    }

    @Test
    void createTrip_rejectsCrossWarehouseDeliveryOrder() {
        stubCreateUntilOrders();
        DeliveryOrder otherWarehouseOrder = order(101L, warehouse(99L), DeliveryOrderStatus.WAREHOUSE_APPROVED);
        when(deliveryOrderRepository.findDetailedByIdIn(List.of(101L))).thenReturn(List.of(otherWarehouseOrder));

        assertThatThrownBy(() -> service.createTrip(createRequest(101L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("TRIP_DO_WAREHOUSE_MISMATCH");
        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTrip_rejectsUnavailableVehicle() {
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        when(vehicleRepository.findWithWarehouseById(301L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.createTrip(createRequest(101L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("VEHICLE_NOT_AVAILABLE");
    }

    @Test
    void createTrip_rejectsDuplicateStopOrder() {
        stubCreateUntilOrders();
        TripCreateRequest request = createRequest(101L);
        TripDeliveryOrderRequest second = tripRow(102L, 1);
        request.setDeliveryOrders(List.of(request.getDeliveryOrders().get(0), second));

        assertThatThrownBy(() -> service.createTrip(request, dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DUPLICATE_STOP_ORDER");
    }

    @Test
    void createTrip_rejectsActiveDeliveryOrderConflict() {
        stubCreateUntilOrders();
        when(tripDeliveryOrderRepository.existsActiveAssignmentForAnyDeliveryOrder(
                eq(List.of(101L)), any(), eq(null))).thenReturn(true);

        assertThatThrownBy(() -> service.createTrip(createRequest(101L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("DO_ALREADY_ASSIGNED_TO_TRIP");
    }

    @Test
    void createTrip_rejectsWeightOverload() {
        stubCreateUntilOrders();
        when(deliveryOrderRepository.findDetailedByIdIn(List.of(101L))).thenReturn(List.of(order));
        vehicle.setMaxWeightKg(new BigDecimal("5.00"));
        when(deliveryOrderItemRepository.findByDeliveryOrderIdIn(List.of(101L)))
                .thenReturn(List.of(item(order, new BigDecimal("10.00"))));

        assertThatThrownBy(() -> service.createTrip(createRequest(101L), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("VEHICLE_OVERLOAD");
    }

    @Test
    void updateTrip_revalidatesListAndIgnoresCurrentTripConflict() {
        Trip trip = plannedTrip();
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(vehicleRepository.findWithWarehouseById(301L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findWithWarehouseAndUserById(401L)).thenReturn(Optional.of(driver));
        when(deliveryOrderRepository.findDetailedByIdIn(List.of(101L))).thenReturn(List.of(order));
        when(deliveryOrderItemRepository.findByDeliveryOrderIdIn(List.of(101L)))
                .thenReturn(List.of(item(order, BigDecimal.ONE)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        var response = service.updateTrip(900L, updateRequest(), dispatcher);

        assertThat(response.getStatus()).isEqualTo(TripStatus.PLANNED);
        verify(tripDeliveryOrderRepository).deleteByTripId(900L);
        verify(tripDeliveryOrderRepository).existsActiveAssignmentForAnyDeliveryOrder(
                eq(List.of(101L)), any(), eq(900L));
    }

    @Test
    void cancelTrip_onlyWorksFromPlanned() {
        Trip trip = plannedTrip();
        trip.setStatus(TripStatus.IN_TRANSIT);
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.cancelTrip(900L, cancelRequest(), dispatcher))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("TRIP_NOT_EDITABLE");
    }

    @Test
    void departTrip_rejectsWhenDeliveryOrderNoLongerWarehouseApproved() {
        Trip trip = plannedTrip();
        order.setStatus(DeliveryOrderStatus.NEW);
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        assertThatThrownBy(() -> service.departTrip(900L, new com.wms.dto.request.TripDepartRequest(), driverUser))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("TRIP_NOT_READY_TO_DEPART");
    }

    @Test
    void departTrip_movesStagingStockAndCreatesDeliveryAttempt() {
        Trip trip = plannedTrip();
        Batch batch = batch(71L);
        WarehouseLocation staging = location(880L, warehouse);
        Warehouse transitWarehouse = warehouse(999L);
        WarehouseLocation transitLocation = location(990L, transitWarehouse);
        DeliveryOrderItem item = item(order, new BigDecimal("10.00"));
        item.setBatch(batch);
        Inventory stagingInventory = inventory(700L, warehouse, batch, staging, new BigDecimal("10.00"));
        OutboundQcRecord record = qcRecord(order, item, batch, staging, new BigDecimal("10.00"));

        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));
        when(deliveryOrderItemRepository.findByDeliveryOrderIdIn(List.of(101L))).thenReturn(List.of(item));
        when(warehouseRepository.findFirstByTypeAndIsActiveTrue(any())).thenReturn(Optional.of(transitWarehouse));
        when(warehouseLocationRepository.findFirstByWarehouseIdAndIsActiveTrue(999L)).thenReturn(Optional.of(transitLocation));
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(101L))).thenReturn(List.of(record));
        when(inventoryRepository.findConcreteRowForTripMovement(20L, 30L, 71L, 880L))
                .thenReturn(Optional.of(stagingInventory));
        when(inventoryRepository.findConcreteRowForTripMovement(999L, 30L, 71L, 990L))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.findMaxAttemptNumberByDeliveryOrderId(101L)).thenReturn(0);
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.departTrip(900L, new com.wms.dto.request.TripDepartRequest(), driverUser);

        assertThat(response.getStatus()).isEqualTo(TripStatus.IN_TRANSIT);
        assertThat(stagingInventory.getTotalQty()).isEqualByComparingTo("0.00");
        assertThat(item.getIssuedQty()).isEqualByComparingTo("10.00");
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ON_TRIP);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.ON_TRIP);
        verify(deliveryRepository).save(any());
    }

    @Test
    void departTrip_rejectsWhenStagedQcPassRecordsDoNotCoverRequestedQty() {
        Trip trip = plannedTrip();
        DeliveryOrderItem item = item(order, new BigDecimal("10.00"));
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));
        when(deliveryOrderItemRepository.findByDeliveryOrderIdIn(List.of(101L))).thenReturn(List.of(item));
        when(warehouseRepository.findFirstByTypeAndIsActiveTrue(any())).thenReturn(Optional.of(warehouse(999L)));
        when(warehouseLocationRepository.findFirstByWarehouseIdAndIsActiveTrue(999L))
                .thenReturn(Optional.of(location(990L, warehouse(999L))));
        when(outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(List.of(101L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.departTrip(900L, new com.wms.dto.request.TripDepartRequest(), driverUser))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("STAGED_QC_PASS_QTY_INSUFFICIENT");
    }


    @Test
    void completeTrip_rejectsWhenAnyDeliveryOrderIsNotTerminal() {
        Trip trip = plannedTrip();
        trip.setStatus(TripStatus.IN_TRANSIT);
        order.setStatus(DeliveryOrderStatus.IN_TRANSIT);
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));

        assertThatThrownBy(() -> service.completeTrip(900L, new com.wms.dto.request.TripCompleteRequest(), driverUser))
                .isInstanceOf(OutboundDeliveryException.class)
                .extracting("code")
                .isEqualTo("TRIP_NOT_READY_TO_COMPLETE");
    }

    @Test
    void completeTrip_releasesVehicleAndDriver() {
        Trip trip = plannedTrip();
        trip.setStatus(TripStatus.IN_TRANSIT);
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        driver.setStatus(DriverStatus.ON_TRIP);
        order.setStatus(DeliveryOrderStatus.COMPLETED);
        when(tripRepository.findWithWarehouseAndResourcesById(900L)).thenReturn(Optional.of(trip));
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenReturn(List.of(member(trip, order, 1)));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.completeTrip(900L, new com.wms.dto.request.TripCompleteRequest(), driverUser);

        assertThat(response.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(driver.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
    }

    private void stubCreateHappyPath() {
        stubCreateUntilOrders();
        when(deliveryOrderRepository.findDetailedByIdIn(List.of(101L))).thenReturn(List.of(order));
        when(deliveryOrderItemRepository.findByDeliveryOrderIdIn(List.of(101L)))
                .thenReturn(List.of(item(order, BigDecimal.ONE)));
        when(tripRepository.existsByTripNumber(any())).thenReturn(false);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(900L);
            return trip;
        });
        when(tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(900L))
                .thenAnswer(inv -> List.of(member(plannedTrip(), order, 1)));
    }

    private void stubCreateUntilOrders() {
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(assignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(20L));
        when(vehicleRepository.findWithWarehouseById(301L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findWithWarehouseAndUserById(401L)).thenReturn(Optional.of(driver));
    }

    private TripCreateRequest createRequest(Long doId) {
        TripCreateRequest request = new TripCreateRequest();
        request.setWarehouseId(20L);
        request.setVehicleId(301L);
        request.setDriverId(401L);
        request.setPlannedStartAt(LocalDateTime.of(2026, 6, 22, 8, 0));
        request.setPlannedEndAt(LocalDateTime.of(2026, 6, 22, 17, 0));
        request.setDeliveryOrders(List.of(tripRow(doId, 1)));
        return request;
    }

    private TripUpdateRequest updateRequest() {
        TripUpdateRequest request = new TripUpdateRequest();
        request.setVehicleId(301L);
        request.setDriverId(401L);
        request.setDeliveryOrders(List.of(tripRow(101L, 1)));
        return request;
    }

    private TripCancelRequest cancelRequest() {
        TripCancelRequest request = new TripCancelRequest();
        request.setReason("Dealer postponed route");
        return request;
    }

    private TripDeliveryOrderRequest tripRow(Long doId, int stopOrder) {
        TripDeliveryOrderRequest request = new TripDeliveryOrderRequest();
        request.setDoId(doId);
        request.setStopOrder(stopOrder);
        return request;
    }

    private Trip plannedTrip() {
        return Trip.builder()
                .id(900L)
                .tripNumber("TRIP-20260620-0001")
                .warehouse(warehouse)
                .vehicle(vehicle)
                .driver(driver)
                .dispatcher(dispatcher)
                .plannedDate(LocalDate.of(2026, 6, 22))
                .tripType(TripType.DELIVERY)
                .status(TripStatus.PLANNED)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private TripDeliveryOrder member(Trip trip, DeliveryOrder order, int stopOrder) {
        return TripDeliveryOrder.builder()
                .trip(trip)
                .deliveryOrder(order)
                .stopOrder(stopOrder)
                .build();
    }

    private DeliveryOrderItem item(DeliveryOrder order, BigDecimal qty) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setDeliveryOrder(order);
        item.setProduct(product);
        item.setRequestedQty(qty);
        item.setQcPassQty(qty);
        item.setIssuedQty(BigDecimal.ZERO);
        return item;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setWeightKg(BigDecimal.ONE);
        product.setVolumeM3(BigDecimal.ONE);
        return product;
    }

    private Batch batch(Long id) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProduct(product);
        batch.setWarehouse(warehouse);
        return batch;
    }

    private WarehouseLocation location(Long id, Warehouse warehouse) {
        WarehouseLocation location = new WarehouseLocation();
        location.setId(id);
        location.setWarehouse(warehouse);
        location.setIsActive(true);
        location.setIsQuarantine(false);
        return location;
    }

    private Inventory inventory(Long id, Warehouse warehouse, Batch batch,
                                WarehouseLocation location, BigDecimal qty) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setWarehouse(warehouse);
        inventory.setProduct(product);
        inventory.setBatch(batch);
        inventory.setLocation(location);
        inventory.setTotalQty(qty);
        inventory.setReservedQty(qty);
        inventory.setCostPrice(BigDecimal.ONE);
        inventory.setUpdatedAt(OffsetDateTime.now());
        return inventory;
    }

    private OutboundQcRecord qcRecord(DeliveryOrder order, DeliveryOrderItem item,
                                      Batch batch, WarehouseLocation staging, BigDecimal qty) {
        OutboundQcRecord record = new OutboundQcRecord();
        record.setDeliveryOrder(order);
        record.setDeliveryOrderItem(item);
        record.setBatch(batch);
        record.setStagingLocation(staging);
        record.setQcPassQty(qty);
        return record;
    }

    private DeliveryOrder order(Long id, Warehouse warehouse, DeliveryOrderStatus status) {
        DeliveryOrder order = new DeliveryOrder();
        order.setId(id);
        order.setDoNumber("DO-" + id);
        order.setWarehouse(warehouse);
        order.setStatus(status);
        return order;
    }

    private Vehicle vehicle(Long id, Warehouse warehouse, VehicleStatus status) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setWarehouse(warehouse);
        vehicle.setStatus(status);
        vehicle.setIsActive(true);
        vehicle.setMaxWeightKg(new BigDecimal("100.00"));
        return vehicle;
    }

    private Driver driver(Long id, Warehouse warehouse, DriverStatus status, User user) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setWarehouse(warehouse);
        driver.setStatus(status);
        driver.setIsActive(true);
        driver.setUser(user);
        return driver;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setIsActive(true);
        return warehouse;
    }

    private User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName(role.name());
        return user;
    }
}
