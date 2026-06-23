package com.wms.service.impl;

import com.wms.dto.request.TripCancelRequest;
import com.wms.dto.request.TripCompleteRequest;
import com.wms.dto.request.TripCreateRequest;
import com.wms.dto.request.TripDeliveryOrderRequest;
import com.wms.dto.request.TripDepartRequest;
import com.wms.dto.request.TripUpdateRequest;
import com.wms.dto.response.TripDeliveryOrderResponse;
import com.wms.dto.response.TripResponse;
import com.wms.entity.Delivery;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.Driver;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundQcRecord;
import com.wms.entity.Trip;
import com.wms.entity.TripDeliveryOrder;
import com.wms.entity.User;
import com.wms.entity.Vehicle;
import com.wms.entity.Warehouse;
import com.wms.entity.WarehouseLocation;
import com.wms.enums.AuditAction;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryStatus;
import com.wms.enums.DriverStatus;
import com.wms.enums.TripStatus;
import com.wms.enums.TripType;
import com.wms.enums.VehicleStatus;
import com.wms.enums.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
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
import com.wms.service.AuditLogService;
import com.wms.service.TripService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripServiceImpl implements TripService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(TripStatus.PLANNED, TripStatus.IN_TRANSIT);
    private static final List<DeliveryOrderStatus> TERMINAL_DO_STATUSES =
            List.of(DeliveryOrderStatus.COMPLETED, DeliveryOrderStatus.RETURNED);

    private final TripRepository tripRepository;
    private final TripDeliveryOrderRepository tripDeliveryOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboundQcRecordRepository outboundQcRecordRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public TripServiceImpl(TripRepository tripRepository,
                           TripDeliveryOrderRepository tripDeliveryOrderRepository,
                           DeliveryRepository deliveryRepository,
                           DeliveryOrderRepository deliveryOrderRepository,
                           DeliveryOrderItemRepository deliveryOrderItemRepository,
                           VehicleRepository vehicleRepository,
                           DriverRepository driverRepository,
                           WarehouseRepository warehouseRepository,
                           WarehouseLocationRepository warehouseLocationRepository,
                           InventoryRepository inventoryRepository,
                           OutboundQcRecordRepository outboundQcRecordRepository,
                           UserWarehouseAssignmentRepository assignmentRepository,
                           AuditLogService auditLogService) {
        this.tripRepository = tripRepository;
        this.tripDeliveryOrderRepository = tripDeliveryOrderRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.inventoryRepository = inventoryRepository;
        this.outboundQcRecordRepository = outboundQcRecordRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> listTrips(Long warehouseId, TripStatus status, User actor) {
        List<Long> warehouseIds;
        if (warehouseId == null) {
            warehouseIds = assignmentRepository.findWarehouseIdsByUserId(actor.getId());
        } else {
            requireWarehouseScope(actor, warehouseId);
            warehouseIds = List.of(warehouseId);
        }
        if (warehouseIds.isEmpty()) {
            return List.of();
        }
        return tripRepository.findByWarehouseIdInAndOptionalStatus(warehouseIds, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TripResponse createTrip(TripCreateRequest request, User actor) {
        Warehouse warehouse = activeWarehouse(request.getWarehouseId());
        requireWarehouseScope(actor, warehouse.getId());
        Vehicle vehicle = availableVehicle(request.getVehicleId(), warehouse.getId(), null);
        Driver driver = availableDriver(request.getDriverId(), warehouse.getId(), null);
        List<DeliveryOrder> orders = validateOrders(request.getDeliveryOrders(), warehouse.getId(), null);
        Capacity capacity = calculateCapacity(orders, vehicle);

        OffsetDateTime now = OffsetDateTime.now();
        Trip trip = Trip.builder()
                .tripNumber(generateTripNumber())
                .warehouse(warehouse)
                .vehicle(vehicle)
                .driver(driver)
                .dispatcher(actor)
                .plannedStartAt(request.getPlannedStartAt())
                .plannedEndAt(request.getPlannedEndAt())
                .tripType(TripType.DELIVERY)
                .status(TripStatus.PLANNED)
                .totalWeightKg(capacity.weight())
                .totalVolumeM3(capacity.volume())
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .build();
        Trip saved = tripRepository.save(trip);
        saveMembership(saved, request.getDeliveryOrders(), orders);
        auditTrip(actor, AuditAction.TRIP_CREATE, saved, null, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TripResponse updateTrip(Long id, TripUpdateRequest request, User actor) {
        Trip trip = loadTrip(id);
        requirePlanned(trip);
        requireWarehouseScope(actor, trip.getWarehouse().getId());
        Map<String, Object> before = snapshotWithMembers(trip);

        Long vehicleId = request.getVehicleId() == null ? trip.getVehicle().getId() : request.getVehicleId();
        Long driverId = request.getDriverId() == null ? trip.getDriver().getId() : request.getDriverId();
        Vehicle vehicle = availableVehicle(vehicleId, trip.getWarehouse().getId(), trip.getId());
        Driver driver = availableDriver(driverId, trip.getWarehouse().getId(), trip.getId());
        List<TripDeliveryOrderRequest> rows = request.getDeliveryOrders() == null
                ? currentRows(trip.getId())
                : request.getDeliveryOrders();
        List<DeliveryOrder> orders = validateOrders(rows, trip.getWarehouse().getId(), trip.getId());
        Capacity capacity = calculateCapacity(orders, vehicle);

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        if (request.getPlannedStartAt() != null) {
            trip.setPlannedStartAt(request.getPlannedStartAt());
        }
        if (request.getPlannedEndAt() != null) {
            trip.setPlannedEndAt(request.getPlannedEndAt());
        }
        trip.setNotes(request.getNotes());
        trip.setTotalWeightKg(capacity.weight());
        trip.setTotalVolumeM3(capacity.volume());
        trip.setUpdatedAt(OffsetDateTime.now());
        tripDeliveryOrderRepository.deleteByTripId(trip.getId());
        saveMembership(trip, rows, orders);
        Trip saved = tripRepository.save(trip);
        auditTrip(actor, AuditAction.TRIP_UPDATE, saved, before, snapshotWithMembers(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TripResponse cancelTrip(Long id, TripCancelRequest request, User actor) {
        Trip trip = loadTrip(id);
        requirePlanned(trip);
        requireWarehouseScope(actor, trip.getWarehouse().getId());
        Map<String, Object> before = snapshotWithMembers(trip);
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancelReason(request.getReason());
        trip.setUpdatedAt(OffsetDateTime.now());
        Trip saved = tripRepository.save(trip);
        auditTrip(actor, AuditAction.TRIP_CANCEL, saved, before, snapshotWithMembers(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TripResponse departTrip(Long id, TripDepartRequest request, User actor) {
        Trip trip = loadTrip(id);
        requireAssignedDriver(actor, trip);
        requirePlanned(trip);
        Map<String, Object> before = snapshotWithMembers(trip);
        List<TripDeliveryOrder> rows = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId());
        List<DeliveryOrder> orders = rows.stream().map(TripDeliveryOrder::getDeliveryOrder).toList();
        validateDepartureOrders(orders);
        moveStagedInventoryToTransit(orders);

        OffsetDateTime now = request.getConfirmedAt() == null ? OffsetDateTime.now() : request.getConfirmedAt();
        for (DeliveryOrder order : orders) {
            order.setStatus(DeliveryOrderStatus.IN_TRANSIT);
            order.setUpdatedAt(now);
            deliveryOrderRepository.save(order);
            createDeliveryAttempt(trip, order, now);
        }
        trip.getVehicle().setStatus(VehicleStatus.ON_TRIP);
        trip.getDriver().setStatus(DriverStatus.ON_TRIP);
        trip.setStatus(TripStatus.IN_TRANSIT);
        trip.setDepartedAt(now);
        trip.setNotes(mergeNotes(trip.getNotes(), request.getNotes()));
        trip.setUpdatedAt(now);
        Trip saved = tripRepository.save(trip);
        auditTrip(actor, AuditAction.TRIP_DEPART, saved, before, snapshotWithMembers(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TripResponse completeTrip(Long id, TripCompleteRequest request, User actor) {
        Trip trip = loadTrip(id);
        requireAssignedDriver(actor, trip);
        if (trip.getStatus() != TripStatus.IN_TRANSIT) {
            throw rule("TRIP_NOT_READY_TO_COMPLETE", "Trip must be IN_TRANSIT before completion");
        }
        List<DeliveryOrder> orders = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId())
                .stream().map(TripDeliveryOrder::getDeliveryOrder).toList();
        if (orders.stream().anyMatch(order -> !TERMINAL_DO_STATUSES.contains(order.getStatus()))) {
            throw rule("TRIP_NOT_READY_TO_COMPLETE", "All delivery orders must be COMPLETED or RETURNED");
        }
        Map<String, Object> before = snapshotWithMembers(trip);
        OffsetDateTime now = request.getReturnedAt() == null ? OffsetDateTime.now() : request.getReturnedAt();
        trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        trip.getDriver().setStatus(DriverStatus.AVAILABLE);
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(now);
        trip.setNotes(mergeNotes(trip.getNotes(), request.getNotes()));
        trip.setUpdatedAt(now);
        Trip saved = tripRepository.save(trip);
        auditTrip(actor, AuditAction.COMPLETE_TRIP, saved, before, snapshotWithMembers(saved));
        return toResponse(saved);
    }

    private List<DeliveryOrder> validateOrders(List<TripDeliveryOrderRequest> rows,
                                               Long warehouseId,
                                               Long excludedTripId) {
        validateStopOrders(rows);
        List<Long> ids = rows.stream().map(TripDeliveryOrderRequest::getDoId).distinct().toList();
        if (ids.size() != rows.size()) {
            throw rule("DUPLICATE_DELIVERY_ORDER", "Delivery orders must be unique within a trip");
        }
        if (tripDeliveryOrderRepository.existsActiveAssignmentForAnyDeliveryOrder(ids, ACTIVE_TRIP_STATUSES, excludedTripId)) {
            throw conflict("DO_ALREADY_ASSIGNED_TO_TRIP", "Delivery order already belongs to an active trip");
        }
        Map<Long, DeliveryOrder> orders = deliveryOrderRepository.findDetailedByIdIn(ids).stream()
                .collect(Collectors.toMap(DeliveryOrder::getId, Function.identity()));
        if (orders.size() != ids.size()) {
            throw notFound("Delivery order not found");
        }
        for (DeliveryOrder order : orders.values()) {
            if (!Objects.equals(order.getWarehouse().getId(), warehouseId)) {
                throw rule("TRIP_DO_WAREHOUSE_MISMATCH", "All delivery orders must belong to the trip warehouse");
            }
            if (order.getStatus() != DeliveryOrderStatus.WAREHOUSE_APPROVED) {
                throw rule("TRIP_DO_STATUS_INVALID", "Delivery order must be WAREHOUSE_APPROVED");
            }
        }
        return ids.stream().map(orders::get).toList();
    }

    private void moveStagedInventoryToTransit(List<DeliveryOrder> orders) {
        List<Long> orderIds = orders.stream().map(DeliveryOrder::getId).toList();
        Map<Long, List<DeliveryOrderItem>> itemsByOrder = deliveryOrderItemRepository.findByDeliveryOrderIdIn(orderIds)
                .stream().collect(Collectors.groupingBy(i -> i.getDeliveryOrder().getId()));
        validateFullQcPass(orders, itemsByOrder);
        Warehouse transitWarehouse = warehouseRepository.findFirstByTypeAndIsActiveTrue(WarehouseType.IN_TRANSIT)
                .orElseThrow(() -> rule("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED", "In-transit warehouse is not configured"));
        WarehouseLocation transitLocation = warehouseLocationRepository
                .findFirstByWarehouseIdAndIsActiveTrue(transitWarehouse.getId())
                .orElseThrow(() -> rule("IN_TRANSIT_LOCATION_NOT_CONFIGURED", "In-transit location is not configured"));
        List<OutboundQcRecord> records = outboundQcRecordRepository.findPassedRecordsByDeliveryOrderIdIn(orderIds);
        BigDecimal movedQty = ZERO;
        for (OutboundQcRecord record : records) {
            movedQty = movedQty.add(moveRecord(record, transitWarehouse, transitLocation));
        }
        BigDecimal requiredQty = itemsByOrder.values().stream()
                .flatMap(Collection::stream)
                .map(DeliveryOrderItem::getRequestedQty)
                .reduce(ZERO, BigDecimal::add);
        if (movedQty.compareTo(requiredQty) != 0) {
            throw rule("STAGED_QC_PASS_QTY_INSUFFICIENT", "Staged QC-pass quantity must equal requested quantity");
        }
        itemsByOrder.values().stream()
                .flatMap(Collection::stream)
                .forEach(item -> item.setIssuedQty(value(item.getRequestedQty())));
        itemsByOrder.values().forEach(deliveryOrderItemRepository::saveAll);
    }

    private BigDecimal moveRecord(OutboundQcRecord record, Warehouse transitWarehouse, WarehouseLocation transitLocation) {
        BigDecimal qty = value(record.getQcPassQty());
        if (qty.compareTo(ZERO) <= 0 || record.getStagingLocation() == null) {
            return ZERO;
        }
        Inventory staging = inventoryRepository.findConcreteRowForTripMovement(
                        record.getDeliveryOrder().getWarehouse().getId(),
                        record.getDeliveryOrderItem().getProduct().getId(),
                        record.getBatch().getId(),
                        record.getStagingLocation().getId())
                .orElseThrow(() -> notFound("Staging inventory not found"));
        staging.setTotalQty(subtract(staging.getTotalQty(), qty, "STAGED_QC_PASS_QTY_INSUFFICIENT"));
        staging.setReservedQty(subtract(staging.getReservedQty(), qty, "STAGED_QC_PASS_QTY_INSUFFICIENT"));
        staging.setUpdatedAt(OffsetDateTime.now());
        saveInventory(staging);
        Inventory transit = inventoryRepository.findConcreteRowForTripMovement(
                        transitWarehouse.getId(), record.getDeliveryOrderItem().getProduct().getId(),
                        record.getBatch().getId(), transitLocation.getId())
                .orElseGet(() -> newTransitInventory(record, transitWarehouse, transitLocation, staging));
        transit.setTotalQty(value(transit.getTotalQty()).add(qty));
        transit.setReservedQty(value(transit.getReservedQty()));
        transit.setUpdatedAt(OffsetDateTime.now());
        saveInventory(transit);
        return qty;
    }

    private void createDeliveryAttempt(Trip trip, DeliveryOrder order, OffsetDateTime now) {
        int attempt = deliveryRepository.findMaxAttemptNumberByDeliveryOrderId(order.getId()) + 1;
        Delivery delivery = Delivery.builder()
                .deliveryNumber("DLV-" + order.getDoNumber() + "-" + attempt)
                .deliveryOrder(order)
                .trip(trip)
                .vehicle(trip.getVehicle())
                .driver(trip.getDriver())
                .attemptNumber(attempt)
                .status(DeliveryStatus.IN_TRANSIT)
                .dispatchedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Delivery saved = deliveryRepository.save(delivery);
        auditLogService.log(trip.getDriver().getUser(), AuditAction.DELIVERY_ATTEMPT_CREATE, "DELIVERY",
                saved.getId(), saved.getDeliveryNumber(), trip.getWarehouse().getId(), null,
                Map.of("tripId", trip.getId(), "deliveryOrderId", order.getId()));
    }

    private void saveMembership(Trip trip, List<TripDeliveryOrderRequest> rows, List<DeliveryOrder> orders) {
        Map<Long, DeliveryOrder> byId = orders.stream().collect(Collectors.toMap(DeliveryOrder::getId, Function.identity()));
        List<TripDeliveryOrder> members = rows.stream()
                .sorted(Comparator.comparing(TripDeliveryOrderRequest::getStopOrder))
                .map(row -> TripDeliveryOrder.builder()
                        .trip(trip)
                        .deliveryOrder(byId.get(row.getDoId()))
                        .stopOrder(row.getStopOrder())
                        .build())
                .toList();
        tripDeliveryOrderRepository.saveAll(members);
    }

    private Capacity calculateCapacity(List<DeliveryOrder> orders, Vehicle vehicle) {
        List<DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderIdIn(
                orders.stream().map(DeliveryOrder::getId).toList());
        BigDecimal weight = ZERO;
        BigDecimal volume = ZERO;
        for (DeliveryOrderItem item : items) {
            BigDecimal qty = value(item.getRequestedQty());
            weight = weight.add(value(item.getProduct().getWeightKg()).multiply(qty));
            volume = volume.add(value(item.getProduct().getVolumeM3()).multiply(qty));
        }
        if (vehicle.getMaxWeightKg() != null && weight.compareTo(vehicle.getMaxWeightKg()) > 0) {
            throw rule("VEHICLE_OVERLOAD", "Trip weight exceeds vehicle capacity");
        }
        if (vehicle.getMaxVolumeM3() != null && volume.compareTo(vehicle.getMaxVolumeM3()) > 0) {
            throw rule("VEHICLE_OVERLOAD", "Trip volume exceeds vehicle capacity");
        }
        return new Capacity(weight, volume);
    }

    private void validateFullQcPass(List<DeliveryOrder> orders, Map<Long, List<DeliveryOrderItem>> itemsByOrder) {
        for (DeliveryOrder order : orders) {
            for (DeliveryOrderItem item : itemsByOrder.getOrDefault(order.getId(), List.of())) {
                if (value(item.getQcPassQty()).compareTo(value(item.getRequestedQty())) != 0) {
                    throw rule("STAGED_QC_PASS_QTY_INSUFFICIENT", "QC-passed quantity must fully cover requested quantity");
                }
            }
        }
    }

    private void validateDepartureOrders(List<DeliveryOrder> orders) {
        if (orders.isEmpty()) {
            throw rule("TRIP_NOT_READY_TO_DEPART", "Trip has no delivery orders");
        }
        for (DeliveryOrder order : orders) {
            if (order.getStatus() != DeliveryOrderStatus.WAREHOUSE_APPROVED) {
                throw rule("TRIP_NOT_READY_TO_DEPART", "All delivery orders must still be WAREHOUSE_APPROVED");
            }
        }
    }

    private Vehicle availableVehicle(Long vehicleId, Long warehouseId, Long excludedTripId) {
        Vehicle vehicle = vehicleRepository.findWithWarehouseById(vehicleId)
                .orElseThrow(() -> notFound("Vehicle not found"));
        if (!Objects.equals(vehicle.getWarehouse().getId(), warehouseId)
                || !Boolean.TRUE.equals(vehicle.getIsActive())
                || vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw rule("VEHICLE_NOT_AVAILABLE", "Vehicle is not available in the selected warehouse");
        }
        if (tripRepository.existsActiveVehicleAssignment(vehicleId, ACTIVE_TRIP_STATUSES, excludedTripId)) {
            throw conflict("VEHICLE_ALREADY_ASSIGNED_TO_TRIP", "Vehicle belongs to another active trip");
        }
        return vehicle;
    }

    private Driver availableDriver(Long driverId, Long warehouseId, Long excludedTripId) {
        Driver driver = driverRepository.findWithWarehouseAndUserById(driverId)
                .orElseThrow(() -> notFound("Driver not found"));
        if (!Objects.equals(driver.getWarehouse().getId(), warehouseId)
                || !Boolean.TRUE.equals(driver.getIsActive())
                || driver.getStatus() != DriverStatus.AVAILABLE) {
            throw rule("DRIVER_NOT_AVAILABLE", "Driver is not available in the selected warehouse");
        }
        if (tripRepository.existsActiveDriverAssignment(driverId, ACTIVE_TRIP_STATUSES, excludedTripId)) {
            throw conflict("DRIVER_ALREADY_ASSIGNED_TO_TRIP", "Driver belongs to another active trip");
        }
        return driver;
    }

    private Trip loadTrip(Long id) {
        return tripRepository.findWithWarehouseAndResourcesById(id)
                .orElseThrow(() -> notFound("Trip not found"));
    }

    private Warehouse activeWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> notFound("Warehouse not found"));
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw rule("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        return warehouse;
    }

    private void requireWarehouseScope(User actor, Long warehouseId) {
        if (!assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId)) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "User is not assigned to warehouse: " + warehouseId);
        }
    }

    private void requireAssignedDriver(User actor, Trip trip) {
        if (trip.getDriver().getUser() == null || !Objects.equals(trip.getDriver().getUser().getId(), actor.getId())) {
            throw new OutboundDeliveryException("TRIP_DRIVER_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "Authenticated driver is not assigned to this trip");
        }
    }

    private void requirePlanned(Trip trip) {
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw rule("TRIP_NOT_EDITABLE", "Trip must be PLANNED");
        }
    }

    private void validateStopOrders(List<TripDeliveryOrderRequest> rows) {
        Set<Integer> stopOrders = new HashSet<>();
        for (TripDeliveryOrderRequest row : rows) {
            if (!stopOrders.add(row.getStopOrder())) {
                throw rule("DUPLICATE_STOP_ORDER", "Stop order must be unique within a trip");
            }
        }
    }

    private List<TripDeliveryOrderRequest> currentRows(Long tripId) {
        return tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(tripId).stream()
                .map(row -> {
                    TripDeliveryOrderRequest request = new TripDeliveryOrderRequest();
                    request.setDoId(row.getDeliveryOrder().getId());
                    request.setStopOrder(row.getStopOrder());
                    return request;
                })
                .toList();
    }

    private TripResponse toResponse(Trip trip) {
        List<TripDeliveryOrderResponse> orders = tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId())
                .stream()
                .map(row -> TripDeliveryOrderResponse.builder()
                        .doId(row.getDeliveryOrder().getId())
                        .doNumber(row.getDeliveryOrder().getDoNumber())
                        .warehouseId(row.getDeliveryOrder().getWarehouse().getId())
                        .status(row.getDeliveryOrder().getStatus())
                        .stopOrder(row.getStopOrder())
                        .build())
                .toList();
        return TripResponse.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .warehouseId(trip.getWarehouse().getId())
                .vehicleId(trip.getVehicle().getId())
                .driverId(trip.getDriver().getId())
                .dispatcherId(trip.getDispatcher().getId())
                .plannedStartAt(trip.getPlannedStartAt())
                .plannedEndAt(trip.getPlannedEndAt())
                .tripType(trip.getTripType())
                .status(trip.getStatus())
                .totalWeightKg(trip.getTotalWeightKg())
                .totalVolumeM3(trip.getTotalVolumeM3())
                .cancelReason(trip.getCancelReason())
                .departedAt(trip.getDepartedAt())
                .completedAt(trip.getCompletedAt())
                .notes(trip.getNotes())
                .deliveryOrders(orders)
                .build();
    }

    private void auditTrip(User actor, AuditAction action, Trip trip,
                           Map<String, Object> before, Map<String, Object> after) {
        auditLogService.log(actor, action, "TRIP", trip.getId(), trip.getTripNumber(),
                trip.getWarehouse().getId(), before, after);
    }

    private Map<String, Object> snapshotWithMembers(Trip trip) {
        Map<String, Object> values = snapshot(trip);
        values.put("deliveryOrders", tripDeliveryOrderRepository.findByTripIdOrderByStopOrderAsc(trip.getId())
                .stream()
                .map(row -> Map.of("doId", row.getDeliveryOrder().getId(), "stopOrder", row.getStopOrder()))
                .toList());
        return values;
    }

    private Map<String, Object> snapshot(Trip trip) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", trip.getId());
        values.put("tripNumber", trip.getTripNumber());
        values.put("warehouseId", trip.getWarehouse().getId());
        values.put("vehicleId", trip.getVehicle().getId());
        values.put("driverId", trip.getDriver().getId());
        values.put("status", trip.getStatus());
        values.put("totalWeightKg", trip.getTotalWeightKg());
        values.put("totalVolumeM3", trip.getTotalVolumeM3());
        return values;
    }

    private Inventory newTransitInventory(OutboundQcRecord record,
                                          Warehouse transitWarehouse,
                                          WarehouseLocation transitLocation,
                                          Inventory staging) {
        return Inventory.builder()
                .warehouse(transitWarehouse)
                .product(record.getDeliveryOrderItem().getProduct())
                .batch(record.getBatch())
                .location(transitLocation)
                .totalQty(ZERO)
                .reservedQty(ZERO)
                .costPrice(staging.getCostPrice())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private void saveInventory(Inventory inventory) {
        try {
            inventoryRepository.save(inventory);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new OutboundDeliveryException("INVENTORY_VERSION_CONFLICT", HttpStatus.CONFLICT,
                    "Inventory version conflict during trip departure");
        }
    }

    private String generateTripNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String candidate = "TRIP-" + date + "-" + String.format("%04d", sequence);
            if (!tripRepository.existsByTripNumber(candidate)) {
                return candidate;
            }
        }
        throw rule("TRIP_NUMBER_EXHAUSTED", "Cannot generate trip number for today");
    }

    private String mergeNotes(String current, String extra) {
        if (extra == null || extra.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return extra;
        }
        return current + "\n" + extra;
    }

    private BigDecimal subtract(BigDecimal current, BigDecimal amount, String code) {
        BigDecimal result = value(current).subtract(value(amount));
        if (result.compareTo(ZERO) < 0) {
            throw rule(code, "Inventory quantity cannot become negative");
        }
        return result;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }

    private OutboundDeliveryException conflict(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.CONFLICT, message);
    }

    private OutboundDeliveryException rule(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private record Capacity(BigDecimal weight, BigDecimal volume) {
    }
}
