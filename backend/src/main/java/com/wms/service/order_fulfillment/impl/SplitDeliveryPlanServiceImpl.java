package com.wms.service.order_fulfillment.impl;

import com.wms.dto.request.SplitDeliveryLegItemRequest;
import com.wms.dto.request.SplitDeliveryLegRequest;
import com.wms.dto.request.SplitDeliveryPlanCreateRequest;
import com.wms.dto.request.SplitDeliveryPlanUpdateRequest;
import com.wms.dto.request.SplitLegFailureRequest;
import com.wms.dto.response.SplitDeliveryLegResponse;
import com.wms.dto.response.SplitLegMilestoneResponse;
import com.wms.dto.response.SplitDeliveryPlanResponse;
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
import com.wms.entity.order_fulfillment.TripDeliveryOrder;
import com.wms.entity.stock_control.Inventory;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.entity.warehouse_location.WarehouseLocation;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.driver_management.DriverStatus;
import com.wms.enums.fleet_management.VehicleStatus;
import com.wms.enums.order_fulfillment.DeliveryOrderStatus;
import com.wms.enums.order_fulfillment.DeliveryStatus;
import com.wms.enums.order_fulfillment.SplitDeliveryPlanStatus;
import com.wms.enums.order_fulfillment.TripStatus;
import com.wms.enums.order_fulfillment.TripType;
import com.wms.enums.warehouse_location.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
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
import com.wms.service.order_fulfillment.SplitDeliveryPlanService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
public class SplitDeliveryPlanServiceImpl implements SplitDeliveryPlanService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(TripStatus.PLANNED, TripStatus.IN_TRANSIT);
    private static final List<SplitDeliveryPlanStatus> ACTIVE_SPLIT_STATUSES =
            List.of(SplitDeliveryPlanStatus.PLANNED, SplitDeliveryPlanStatus.IN_TRANSIT);

    private final SplitDeliveryPlanRepository splitPlanRepository;
    private final SplitDeliveryLegRepository splitLegRepository;
    private final SplitDeliveryLegItemRepository splitLegItemRepository;
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

    public SplitDeliveryPlanServiceImpl(SplitDeliveryPlanRepository splitPlanRepository,
            SplitDeliveryLegRepository splitLegRepository,
            SplitDeliveryLegItemRepository splitLegItemRepository,
            TripRepository tripRepository,
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
        this.splitPlanRepository = splitPlanRepository;
        this.splitLegRepository = splitLegRepository;
        this.splitLegItemRepository = splitLegItemRepository;
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
    @Transactional
    public SplitDeliveryPlanResponse createPlan(SplitDeliveryPlanCreateRequest request, User actor) {
        DeliveryOrder order = loadApprovedOrder(request.getDoId());
        requireWarehouseScope(actor, order.getWarehouse().getId());
        validateNoActiveSplitPlan(order.getId(), null);
        validateNoStandardTripAssignment(order.getId(), null);
        validateSchedule(request.getPlannedStartAt(), request.getPlannedEndAt());
        Map<Long, DeliveryOrderItem> items = loadItems(order);
        Allocation allocation = validateAllocation(request.getLegs(), items);
        Driver leadDriver = validateLeadDriver(request.getLeadDriverId(), request.getLegs(), order.getWarehouse().getId(),
                null);
        OffsetDateTime now = OffsetDateTime.now();
        SplitDeliveryPlan plan = SplitDeliveryPlan.builder()
                .planNumber(generatePlanNumber())
                .deliveryOrder(order)
                .warehouse(order.getWarehouse())
                .dispatcher(actor)
                .leadDriver(leadDriver)
                .status(SplitDeliveryPlanStatus.PLANNED)
                .plannedStartAt(request.getPlannedStartAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
        SplitDeliveryPlan saved = splitPlanRepository.save(plan);
        createLegs(saved, request.getLegs(), items, allocation, actor, request.getPlannedStartAt(),
                request.getPlannedEndAt(), now);
        audit(actor, AuditAction.SPLIT_DELIVERY_PLAN_CREATE, saved, null, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SplitDeliveryPlanResponse updatePlan(Long id, SplitDeliveryPlanUpdateRequest request, User actor) {
        SplitDeliveryPlan plan = loadPlan(id);
        requirePlanned(plan);
        requireWarehouseScope(actor, plan.getWarehouse().getId());
        Map<String, Object> before = snapshotWithLegs(plan);
        List<SplitDeliveryLeg> currentLegs = splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId());
        if (currentLegs.stream().anyMatch(leg -> leg.getStatus() != SplitDeliveryPlanStatus.PLANNED)) {
            throw rule("SPLIT_DELIVERY_PLAN_NOT_EDITABLE", "Split plan legs must be PLANNED");
        }
        if (request.getLegs() != null) {
            cancelCurrentLegTrips(currentLegs, actor);
            Map<Long, DeliveryOrderItem> items = loadItems(plan.getDeliveryOrder());
            Allocation allocation = validateAllocation(request.getLegs(), items);
            LocalDateTime plannedStartAt = request.getPlannedStartAt() == null
                    ? plan.getPlannedStartAt()
                    : request.getPlannedStartAt();
            LocalDateTime plannedEndAt = request.getPlannedEndAt() == null
                    ? currentLegs.get(0).getTrip().getPlannedEndAt()
                    : request.getPlannedEndAt();
            validateSchedule(plannedStartAt, plannedEndAt);
            Driver leadDriver = validateLeadDriver(resolveLeadDriverId(request, plan), request.getLegs(),
                    plan.getWarehouse().getId(), plan.getId());
            plan.setLeadDriver(leadDriver);
            createLegs(plan, request.getLegs(), items, allocation, actor, plannedStartAt, plannedEndAt,
                    OffsetDateTime.now());
        } else if (request.getLeadDriverId() != null) {
            Driver leadDriver = validateExistingLeadDriver(request.getLeadDriverId(), currentLegs);
            plan.setLeadDriver(leadDriver);
        }
        if (request.getPlannedStartAt() != null) {
            plan.setPlannedStartAt(request.getPlannedStartAt());
        }
        plan.setUpdatedAt(OffsetDateTime.now());
        SplitDeliveryPlan saved = splitPlanRepository.save(plan);
        audit(actor, AuditAction.SPLIT_DELIVERY_PLAN_UPDATE, saved, before, snapshotWithLegs(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SplitDeliveryPlanResponse confirmDriverReadiness(Long id, User actor) {
        SplitDeliveryPlan plan = loadPlan(id);
        requireSplitDriver(actor, plan);
        requirePlanned(plan);
        SplitDeliveryLeg leg = splitLegRepository.findBySplitPlanIdAndDriverUserId(plan.getId(), actor.getId())
                .orElseThrow(() -> new OutboundDeliveryException("TRIP_DRIVER_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                        "Authenticated driver is not assigned to this split plan"));
        if (leg.getReadinessConfirmedAt() == null) {
            leg.setReadinessConfirmedAt(OffsetDateTime.now());
            leg.setUpdatedAt(leg.getReadinessConfirmedAt());
            splitLegRepository.save(leg);
            audit(actor, AuditAction.SPLIT_DELIVERY_DRIVER_READY, plan, null,
                    Map.of("splitPlanId", plan.getId(), "driverId", leg.getDriver().getId()));
        }
        return toResponse(plan);
    }

    @Override
    @Transactional
    public SplitDeliveryPlanResponse departPlan(Long id, User actor) {
        SplitDeliveryPlan plan = loadPlan(id);
        requireLeadDriver(actor, plan);
        requirePlanned(plan);
        List<SplitDeliveryLeg> legs = splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId());
        departAllLegs(plan, legs, actor);
        return toResponse(plan);
    }

    @Override
    @Transactional
    public SplitLegMilestoneResponse confirmDealerArrival(Long planId, Long legId, User actor) {
        SplitDeliveryPlan plan = loadPlan(planId);
        SplitDeliveryLeg leg = assignedInTransitLeg(plan, legId, actor);
        if (leg.getDealerArrivedAt() == null) {
            OffsetDateTime now = OffsetDateTime.now();
            leg.setDealerArrivedAt(now);
            leg.setUpdatedAt(now);
            splitLegRepository.save(leg);
            auditLeg(actor, AuditAction.SPLIT_LEG_ARRIVAL_CONFIRM, plan, leg, null,
                    Map.of("dealerArrivedAt", now));
        }
        return milestoneResponse(plan, leg);
    }

    @Override
    @Transactional
    public SplitLegMilestoneResponse confirmHandover(Long planId, Long legId, User actor) {
        SplitDeliveryPlan plan = loadPlan(planId);
        SplitDeliveryLeg leg = assignedInTransitLeg(plan, legId, actor);
        List<SplitDeliveryLeg> legs = activeLegs(plan);
        if (legs.stream().anyMatch(item -> item.getDealerArrivedAt() == null)) {
            throw rule("SPLIT_DELIVERY_INCOMPLETE", "Every split leg must arrive before handover");
        }
        if (leg.getHandoverConfirmedAt() == null) {
            OffsetDateTime now = OffsetDateTime.now();
            leg.setHandoverConfirmedAt(now);
            leg.setUpdatedAt(now);
            splitLegRepository.save(leg);
            auditLeg(actor, AuditAction.SPLIT_LEG_HANDOVER_CONFIRM, plan, leg, null,
                    Map.of("handoverConfirmedAt", now));
        }
        return milestoneResponse(plan, leg);
    }

    @Override
    @Transactional
    public SplitLegMilestoneResponse failDeliveryLeg(Long planId, Long legId, SplitLegFailureRequest request,
            User actor) {
        SplitDeliveryPlan plan = loadPlan(planId);
        SplitDeliveryLeg failedLeg = assignedInTransitLeg(plan, legId, actor);
        DeliveryOrder order = plan.getDeliveryOrder();
        if (order.getStatus() != DeliveryOrderStatus.IN_TRANSIT) {
            throw new OutboundDeliveryException("DELIVERY_ALREADY_FINALIZED", HttpStatus.CONFLICT,
                    "Delivery Order is no longer eligible for split failure");
        }
        Map<String, Object> before = snapshotWithLegs(plan);
        OffsetDateTime now = OffsetDateTime.now();
        failedLeg.setFailureReportedAt(now);
        failedLeg.setFailureReason(request.getFailureReason());
        List<SplitDeliveryLeg> legs = activeLegs(plan);
        legs.forEach(leg -> {
            leg.setStatus(SplitDeliveryPlanStatus.RETURNED);
            leg.setUpdatedAt(now);
        });
        Delivery delivery = deliveryRepository.findLatestCurrentAttemptByDeliveryOrderId(
                        order.getId(), List.of(DeliveryStatus.IN_TRANSIT))
                .orElseThrow(() -> notFound("Current split delivery attempt not found"));
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(request.getFailureReason());
        delivery.setUpdatedAt(now);
        order.setStatus(DeliveryOrderStatus.RETURNED);
        order.setUpdatedAt(now);
        plan.setStatus(SplitDeliveryPlanStatus.RETURNED);
        plan.setUpdatedAt(now);
        deliveryRepository.save(delivery);
        deliveryOrderRepository.save(order);
        splitLegRepository.saveAll(legs);
        splitPlanRepository.save(plan);
        auditLeg(actor, AuditAction.SPLIT_LEG_DELIVERY_FAIL, plan, failedLeg, before,
                snapshotWithLegs(plan));
        return milestoneResponse(plan, failedLeg);
    }

    @Override
    @Transactional
    public SplitDeliveryPlanResponse cancelPlan(Long id, String cancelReason, User actor) {
        SplitDeliveryPlan plan = loadPlan(id);
        requirePlanned(plan);
        requireWarehouseScope(actor, plan.getWarehouse().getId());
        Map<String, Object> before = snapshotWithLegs(plan);
        List<SplitDeliveryLeg> legs = splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId());
        cancelCurrentLegTrips(legs, actor);
        OffsetDateTime now = OffsetDateTime.now();
        plan.setStatus(SplitDeliveryPlanStatus.CANCELLED);
        plan.setCancelledAt(now);
        plan.setCancelReason(cancelReason);
        plan.setUpdatedAt(now);
        SplitDeliveryPlan saved = splitPlanRepository.save(plan);
        audit(actor, AuditAction.SPLIT_DELIVERY_PLAN_CANCEL, saved, before, snapshotWithLegs(saved));
        return toResponse(saved);
    }

    private void createLegs(SplitDeliveryPlan plan, List<SplitDeliveryLegRequest> rows,
            Map<Long, DeliveryOrderItem> items, Allocation allocation, User actor,
            LocalDateTime plannedStartAt, LocalDateTime plannedEndAt, OffsetDateTime now) {
        Set<Integer> stopOrders = new LinkedHashSet<>();
        Set<Long> vehicleIds = new LinkedHashSet<>();
        Set<Long> driverIds = new LinkedHashSet<>();
        for (SplitDeliveryLegRequest row : rows) {
            if (!stopOrders.add(row.getStopOrder())) {
                throw rule("DUPLICATE_STOP_ORDER", "Stop order must be unique within split plan");
            }
            if (!vehicleIds.add(row.getVehicleId())) {
                throw rule("DUPLICATE_VEHICLE", "Vehicle must be unique within split plan");
            }
            if (!driverIds.add(row.getDriverId())) {
                throw rule("DUPLICATE_DRIVER", "Driver must be unique within split plan");
            }
            Vehicle vehicle = availableVehicle(row.getVehicleId(), plan.getWarehouse().getId(), plan.getId());
            Driver driver = availableDriver(row.getDriverId(), plan.getWarehouse().getId(), plan.getId());
            Capacity capacity = calculateLegCapacity(row, items);
            validateCapacity(vehicle, capacity);
            Trip trip = createLegTrip(plan, row, vehicle, driver, actor, plannedStartAt, plannedEndAt, capacity, now);
            SplitDeliveryLeg leg = splitLegRepository.save(SplitDeliveryLeg.builder()
                    .splitPlan(plan)
                    .trip(trip)
                    .vehicle(vehicle)
                    .driver(driver)
                    .stopOrder(row.getStopOrder())
                    .status(SplitDeliveryPlanStatus.PLANNED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            saveLegItems(leg, row, items);
        }
    }

    private Trip createLegTrip(SplitDeliveryPlan plan, SplitDeliveryLegRequest row, Vehicle vehicle, Driver driver,
            User actor, LocalDateTime start, LocalDateTime end, Capacity capacity,
            OffsetDateTime now) {
        Trip trip = Trip.builder()
                .tripNumber(generateTripNumber())
                .warehouse(plan.getWarehouse())
                .vehicle(vehicle)
                .driver(driver)
                .dispatcher(actor)
                .plannedDate(start.toLocalDate())
                .plannedStartAt(start)
                .plannedEndAt(end)
                .tripType(TripType.DELIVERY)
                .status(TripStatus.PLANNED)
                .totalWeightKg(capacity.weight())
                .totalVolumeM3(capacity.volume())
                .notes("Split delivery plan " + plan.getPlanNumber())
                .createdAt(now)
                .updatedAt(now)
                .build();
        Trip saved = tripRepository.save(trip);
        tripDeliveryOrderRepository.save(TripDeliveryOrder.builder()
                .trip(saved)
                .deliveryOrder(plan.getDeliveryOrder())
                .splitPlan(plan)
                .stopOrder(row.getStopOrder())
                .isActive(true)
                .build());
        return saved;
    }

    private void saveLegItems(SplitDeliveryLeg leg, SplitDeliveryLegRequest row, Map<Long, DeliveryOrderItem> items) {
        List<SplitDeliveryLegItem> entities = row.getItems().stream()
                .map(item -> {
                    DeliveryOrderItem source = items.get(item.getDoItemId());
                    BigDecimal quantity = value(item.getQuantity());
                    return SplitDeliveryLegItem.builder()
                            .splitLeg(leg)
                            .deliveryOrderItem(source)
                            .product(source.getProduct())
                            .batch(source.getBatch())
                            .quantity(quantity)
                            .weightKg(value(source.getProduct().getWeightKg()).multiply(quantity))
                            .volumeM3(value(source.getProduct().getVolumeM3()).multiply(quantity))
                            .createdAt(OffsetDateTime.now())
                            .build();
                })
                .toList();
        splitLegItemRepository.saveAll(entities);
    }

    private void departAllLegs(SplitDeliveryPlan plan, List<SplitDeliveryLeg> legs, User actor) {
        validateDeparturePlan(plan, legs);
        Map<String, Object> before = snapshotWithLegs(plan);
        List<Long> legIds = legs.stream().map(SplitDeliveryLeg::getId).toList();
        List<SplitDeliveryLegItem> items = splitLegItemRepository.findBySplitLegIdIn(legIds);
        moveSplitInventory(plan, items);
        OffsetDateTime now = OffsetDateTime.now();
        plan.getDeliveryOrder().setStatus(DeliveryOrderStatus.IN_TRANSIT);
        plan.getDeliveryOrder().setUpdatedAt(now);
        deliveryOrderRepository.save(plan.getDeliveryOrder());
        for (SplitDeliveryLeg leg : legs) {
            leg.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
            leg.setDepartedAt(now);
            leg.setUpdatedAt(now);
            leg.getTrip().setStatus(TripStatus.IN_TRANSIT);
            leg.getTrip().setDepartedAt(now);
            leg.getTrip().setUpdatedAt(now);
            leg.getVehicle().setStatus(VehicleStatus.ON_TRIP);
            leg.getDriver().setStatus(DriverStatus.ON_TRIP);
            tripRepository.save(leg.getTrip());
        }
        createLeadDeliveryAttempt(plan, legs, now);
        plan.setStatus(SplitDeliveryPlanStatus.IN_TRANSIT);
        plan.setDepartedAt(now);
        plan.setUpdatedAt(now);
        SplitDeliveryPlan saved = splitPlanRepository.save(plan);
        splitLegRepository.saveAll(legs);
        audit(actor, AuditAction.SPLIT_DELIVERY_DEPART, saved, before, snapshotWithLegs(saved));
    }

    private void moveSplitInventory(SplitDeliveryPlan plan, List<SplitDeliveryLegItem> items) {
        Warehouse transitWarehouse = warehouseRepository.findFirstByTypeAndIsActiveTrue(WarehouseType.IN_TRANSIT)
                .orElseThrow(() -> rule("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED", "In-transit warehouse is not configured"));
        WarehouseLocation transitLocation = warehouseLocationRepository
                .findFirstByWarehouseIdAndIsActiveTrue(transitWarehouse.getId())
                .orElseThrow(() -> rule("IN_TRANSIT_LOCATION_NOT_CONFIGURED", "In-transit location is not configured"));
        List<OutboundQcRecord> records = outboundQcRecordRepository
                .findPassedRecordsByDeliveryOrderIdIn(List.of(plan.getDeliveryOrder().getId()));
        for (SplitDeliveryLegItem item : items) {
            moveLegItem(item, records, transitWarehouse, transitLocation);
            item.getDeliveryOrderItem().setIssuedQty(value(item.getDeliveryOrderItem().getIssuedQty()).add(item.getQuantity()));
        }
        deliveryOrderItemRepository.saveAll(items.stream().map(SplitDeliveryLegItem::getDeliveryOrderItem).toList());
    }

    private void moveLegItem(SplitDeliveryLegItem item, List<OutboundQcRecord> records, Warehouse transitWarehouse,
            WarehouseLocation transitLocation) {
        BigDecimal remaining = item.getQuantity();
        for (OutboundQcRecord record : records) {
            if (remaining.compareTo(ZERO) <= 0 || !matches(item, record)) {
                continue;
            }
            BigDecimal qty = remaining.min(value(record.getQcPassQty()));
            moveRecord(record, qty, transitWarehouse, transitLocation);
            remaining = remaining.subtract(qty);
        }
        if (remaining.compareTo(ZERO) > 0) {
            throw rule("STAGED_QC_PASS_QTY_INSUFFICIENT", "Staged QC-pass quantity is insufficient for split leg");
        }
    }

    private void moveRecord(OutboundQcRecord record, BigDecimal qty, Warehouse transitWarehouse,
            WarehouseLocation transitLocation) {
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
                transitWarehouse.getId(),
                record.getDeliveryOrderItem().getProduct().getId(),
                record.getBatch().getId(),
                transitLocation.getId())
                .orElseGet(() -> newTransitInventory(record, transitWarehouse, transitLocation, staging));
        transit.setTotalQty(value(transit.getTotalQty()).add(qty));
        transit.setReservedQty(value(transit.getReservedQty()));
        transit.setUpdatedAt(OffsetDateTime.now());
        saveInventory(transit);
    }

    private void createLeadDeliveryAttempt(SplitDeliveryPlan plan, List<SplitDeliveryLeg> legs, OffsetDateTime now) {
        SplitDeliveryLeg leadLeg = legs.stream()
                .filter(leg -> Objects.equals(leg.getDriver().getId(), plan.getLeadDriver().getId()))
                .findFirst()
                .orElseThrow(() -> rule("SPLIT_LEAD_DRIVER_REQUIRED", "Lead driver must be assigned to a split leg"));
        int attempt = deliveryRepository.findMaxAttemptNumberByDeliveryOrderId(plan.getDeliveryOrder().getId()) + 1;
        Delivery saved = deliveryRepository.save(Delivery.builder()
                .deliveryNumber("DLV-" + plan.getDeliveryOrder().getDoNumber() + "-" + attempt)
                .deliveryOrder(plan.getDeliveryOrder())
                .trip(leadLeg.getTrip())
                .vehicle(leadLeg.getVehicle())
                .driver(leadLeg.getDriver())
                .attemptNumber(attempt)
                .status(DeliveryStatus.IN_TRANSIT)
                .dispatchedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
        auditLogService.log(plan.getLeadDriver().getUser(), AuditAction.DELIVERY_ATTEMPT_CREATE, "DELIVERY",
                saved.getId(), saved.getDeliveryNumber(), plan.getWarehouse().getId(), null,
                Map.of("splitPlanId", plan.getId(), "deliveryOrderId", plan.getDeliveryOrder().getId()));
    }

    private Allocation validateAllocation(List<SplitDeliveryLegRequest> legs, Map<Long, DeliveryOrderItem> items) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (SplitDeliveryLegRequest leg : legs) {
            for (SplitDeliveryLegItemRequest item : leg.getItems()) {
                DeliveryOrderItem source = items.get(item.getDoItemId());
                if (source == null || !Objects.equals(source.getProduct().getId(), item.getProductId())
                        || source.getBatch() == null || !Objects.equals(source.getBatch().getId(), item.getBatchId())) {
                    throw rule("SPLIT_DELIVERY_PLAN_INVALID", "Split item does not match the Delivery Order item");
                }
                totals.merge(item.getDoItemId(), value(item.getQuantity()), BigDecimal::add);
            }
        }
        for (DeliveryOrderItem item : items.values()) {
            if (value(item.getQcPassQty()).compareTo(value(item.getRequestedQty())) != 0) {
                throw rule("STAGED_QC_PASS_QTY_INSUFFICIENT", "QC-passed quantity must fully cover requested quantity");
            }
            if (value(item.getQcPassQty()).compareTo(value(totals.get(item.getId()))) != 0) {
                throw rule("SPLIT_DELIVERY_INCOMPLETE", "Split plan must allocate 100% of approved quantity");
            }
        }
        return new Allocation(totals);
    }

    private Capacity calculateLegCapacity(SplitDeliveryLegRequest leg, Map<Long, DeliveryOrderItem> items) {
        BigDecimal weight = ZERO;
        BigDecimal volume = ZERO;
        for (SplitDeliveryLegItemRequest item : leg.getItems()) {
            DeliveryOrderItem source = items.get(item.getDoItemId());
            BigDecimal qty = value(item.getQuantity());
            weight = weight.add(value(source.getProduct().getWeightKg()).multiply(qty));
            volume = volume.add(value(source.getProduct().getVolumeM3()).multiply(qty));
        }
        return new Capacity(weight, volume);
    }

    private void validateCapacity(Vehicle vehicle, Capacity capacity) {
        if (vehicle.getMaxWeightKg() != null && capacity.weight().compareTo(vehicle.getMaxWeightKg()) > 0) {
            throw rule("SPLIT_LEG_OVER_CAPACITY", "Split leg weight exceeds vehicle capacity");
        }
        if (vehicle.getMaxVolumeM3() != null && capacity.volume().compareTo(vehicle.getMaxVolumeM3()) > 0) {
            throw rule("SPLIT_LEG_OVER_CAPACITY", "Split leg volume exceeds vehicle capacity");
        }
    }

    private Vehicle availableVehicle(Long vehicleId, Long warehouseId, Long excludedPlanId) {
        Vehicle vehicle = vehicleRepository.findWithWarehouseById(vehicleId)
                .orElseThrow(() -> notFound("Vehicle not found"));
        if (!Objects.equals(vehicle.getWarehouse().getId(), warehouseId)
                || !Boolean.TRUE.equals(vehicle.getIsActive())
                || vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw rule("SPLIT_VEHICLE_NOT_AVAILABLE", "Chờ có xe sẵn sàng để tạo kế hoạch giao hàng");
        }
        if (tripRepository.existsActiveVehicleAssignment(vehicleId, ACTIVE_TRIP_STATUSES, null)
                || splitLegRepository.existsActiveVehicleLeg(vehicleId, ACTIVE_SPLIT_STATUSES, excludedPlanId)) {
            throw rule("SPLIT_VEHICLE_NOT_AVAILABLE", "Chờ có xe sẵn sàng để tạo kế hoạch giao hàng");
        }
        return vehicle;
    }

    private Driver availableDriver(Long driverId, Long warehouseId, Long excludedPlanId) {
        Driver driver = driverRepository.findWithWarehouseAndUserById(driverId)
                .orElseThrow(() -> notFound("Driver not found"));
        if (!Objects.equals(driver.getWarehouse().getId(), warehouseId)
                || !Boolean.TRUE.equals(driver.getIsActive())
                || driver.getStatus() != DriverStatus.AVAILABLE) {
            throw rule("DRIVER_NOT_AVAILABLE", "Driver is not available in the selected warehouse");
        }
        if (driver.getLicenseExpiry() == null || driver.getLicenseExpiry().isBefore(LocalDate.now())) {
            throw rule("DRIVER_LICENSE_EXPIRED", "Driver license is missing or expired");
        }
        if (tripRepository.existsActiveDriverAssignment(driverId, ACTIVE_TRIP_STATUSES, null)
                || splitLegRepository.existsActiveDriverLeg(driverId, ACTIVE_SPLIT_STATUSES, excludedPlanId)) {
            throw rule("DRIVER_ALREADY_ASSIGNED_TO_TRIP", "Driver belongs to another active trip");
        }
        return driver;
    }

    private Driver validateLeadDriver(Long leadDriverId, List<SplitDeliveryLegRequest> legs, Long warehouseId,
            Long excludedPlanId) {
        if (legs.stream().noneMatch(leg -> Objects.equals(leg.getDriverId(), leadDriverId))) {
            throw rule("SPLIT_LEAD_DRIVER_REQUIRED", "Lead driver must be assigned to a split leg");
        }
        return availableDriver(leadDriverId, warehouseId, excludedPlanId);
    }

    private Driver validateExistingLeadDriver(Long leadDriverId, List<SplitDeliveryLeg> legs) {
        return legs.stream()
                .map(SplitDeliveryLeg::getDriver)
                .filter(driver -> Objects.equals(driver.getId(), leadDriverId))
                .findFirst()
                .orElseThrow(() -> rule("SPLIT_LEAD_DRIVER_REQUIRED", "Lead driver must be assigned to a split leg"));
    }

    private void validateDeparturePlan(SplitDeliveryPlan plan, List<SplitDeliveryLeg> legs) {
        if (legs.isEmpty() || legs.stream().anyMatch(leg -> leg.getStatus() != SplitDeliveryPlanStatus.PLANNED)) {
            throw rule("SPLIT_DELIVERY_INCOMPLETE", "All split legs must be PLANNED before departure");
        }
        if (legs.stream().anyMatch(leg -> leg.getReadinessConfirmedAt() == null)) {
            throw rule("SPLIT_DELIVERY_INCOMPLETE", "All split drivers must confirm readiness");
        }
        if (legs.stream().anyMatch(leg -> leg.getVehicle().getStatus() != VehicleStatus.AVAILABLE)) {
            throw rule("SPLIT_VEHICLE_NOT_AVAILABLE", "Cho co xe san sang de tao ke hoach giao hang");
        }
        if (legs.stream().anyMatch(leg -> leg.getDriver().getStatus() != DriverStatus.AVAILABLE)) {
            throw rule("DRIVER_NOT_AVAILABLE", "Driver is not available in the selected warehouse");
        }
        if (plan.getDeliveryOrder().getStatus() != DeliveryOrderStatus.WAREHOUSE_APPROVED) {
            throw rule("TRIP_NOT_READY_TO_DEPART", "Delivery Order must still be WAREHOUSE_APPROVED");
        }
    }

    private void requireLeadDriver(User actor, SplitDeliveryPlan plan) {
        if (plan.getLeadDriver() == null
                || plan.getLeadDriver().getUser() == null
                || !Objects.equals(plan.getLeadDriver().getUser().getId(), actor.getId())) {
            throw new OutboundDeliveryException("SPLIT_LEAD_DRIVER_REQUIRED", HttpStatus.FORBIDDEN,
                    "Only the lead driver can depart a split delivery plan");
        }
    }

    private DeliveryOrder loadApprovedOrder(Long id) {
        DeliveryOrder order = deliveryOrderRepository.findWithDealerAndWarehouseById(id)
                .orElseThrow(() -> notFound("Delivery order not found"));
        if (order.getStatus() != DeliveryOrderStatus.WAREHOUSE_APPROVED) {
            throw rule("DO_NOT_WAREHOUSE_APPROVED", "Delivery Order must be WAREHOUSE_APPROVED");
        }
        return order;
    }

    private Map<Long, DeliveryOrderItem> loadItems(DeliveryOrder order) {
        Map<Long, DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderId(order.getId())
                .stream()
                .collect(Collectors.toMap(DeliveryOrderItem::getId, Function.identity()));
        if (items.isEmpty()) {
            throw rule("SPLIT_DELIVERY_PLAN_INVALID", "Delivery Order has no items");
        }
        return items;
    }

    private SplitDeliveryPlan loadPlan(Long id) {
        return splitPlanRepository.findDetailedById(id)
                .orElseThrow(() -> notFound("Split delivery plan not found"));
    }

    private void requirePlanned(SplitDeliveryPlan plan) {
        if (plan.getStatus() != SplitDeliveryPlanStatus.PLANNED) {
            throw rule("SPLIT_DELIVERY_PLAN_NOT_EDITABLE", "Split delivery plan must be PLANNED");
        }
    }

    private void requireWarehouseScope(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        if (!assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId)) {
            throw new OutboundDeliveryException("WAREHOUSE_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "User is not assigned to warehouse: " + warehouseId);
        }
    }

    private void requireSplitDriver(User actor, SplitDeliveryPlan plan) {
        boolean assigned = splitLegRepository.findBySplitPlanIdAndDriverUserId(plan.getId(), actor.getId()).isPresent();
        if (!assigned) {
            throw new OutboundDeliveryException("TRIP_DRIVER_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "Authenticated driver is not assigned to this split plan");
        }
    }

    private void validateNoActiveSplitPlan(Long deliveryOrderId, Long excludedPlanId) {
        if (splitPlanRepository.existsActivePlanForDeliveryOrder(deliveryOrderId, ACTIVE_SPLIT_STATUSES, excludedPlanId)) {
            throw rule("SPLIT_DELIVERY_PLAN_INVALID", "Delivery Order already has an active split delivery plan");
        }
    }

    private void validateNoStandardTripAssignment(Long deliveryOrderId, Long excludedTripId) {
        if (tripDeliveryOrderRepository.existsActiveAssignmentForAnyDeliveryOrder(List.of(deliveryOrderId),
                ACTIVE_TRIP_STATUSES, excludedTripId)) {
            throw new OutboundDeliveryException("DELIVERY_ORDER_ALREADY_ASSIGNED", HttpStatus.CONFLICT,
                    "Delivery Order is already assigned to another active trip");
        }
    }

    private void validateSchedule(LocalDateTime plannedStartAt, LocalDateTime plannedEndAt) {
        if (plannedStartAt == null || plannedEndAt == null) {
            throw rule("TRIP_SCHEDULE_REQUIRED", "Planned start and end time are required");
        }
        if (!plannedEndAt.isAfter(plannedStartAt)) {
            throw rule("TRIP_SCHEDULE_INVALID", "Planned end time must be after planned start time");
        }
        if (plannedEndAt.isBefore(LocalDateTime.now())) {
            throw rule("TRIP_SCHEDULE_IN_PAST", "Planned end time cannot be in the past");
        }
    }

    private void cancelCurrentLegTrips(List<SplitDeliveryLeg> legs, User actor) {
        OffsetDateTime now = OffsetDateTime.now();
        for (SplitDeliveryLeg leg : legs) {
            leg.setStatus(SplitDeliveryPlanStatus.CANCELLED);
            leg.setUpdatedAt(now);
            leg.getTrip().setStatus(TripStatus.CANCELLED);
            leg.getTrip().setCancelReason("SPLIT_DELIVERY_PLAN_UPDATE");
            leg.getTrip().setUpdatedAt(now);
            tripDeliveryOrderRepository.deactivateByTripId(leg.getTrip().getId());
            tripRepository.save(leg.getTrip());
        }
        splitLegRepository.saveAll(legs);
    }

    private Long resolveLeadDriverId(SplitDeliveryPlanUpdateRequest request, SplitDeliveryPlan plan) {
        return request.getLeadDriverId() == null ? plan.getLeadDriver().getId() : request.getLeadDriverId();
    }

    private boolean matches(SplitDeliveryLegItem item, OutboundQcRecord record) {
        return Objects.equals(record.getDeliveryOrderItem().getId(), item.getDeliveryOrderItem().getId())
                && Objects.equals(record.getBatch().getId(), item.getBatch().getId());
    }

    private Inventory newTransitInventory(OutboundQcRecord record, Warehouse transitWarehouse,
            WarehouseLocation transitLocation, Inventory staging) {
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
                    "Inventory version conflict during split delivery departure");
        }
    }

    private SplitDeliveryPlanResponse toResponse(SplitDeliveryPlan plan) {
        List<SplitDeliveryLeg> legs = splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId()).stream()
                .filter(leg -> leg.getStatus() != SplitDeliveryPlanStatus.CANCELLED)
                .sorted(Comparator.comparing(SplitDeliveryLeg::getStopOrder))
                .toList();
        return SplitDeliveryPlanResponse.builder()
                .id(plan.getId())
                .planNumber(plan.getPlanNumber())
                .doId(plan.getDeliveryOrder().getId())
                .warehouseId(plan.getWarehouse().getId())
                .dispatcherId(plan.getDispatcher().getId())
                .leadDriverId(plan.getLeadDriver().getId())
                .status(plan.getStatus())
                .plannedStartAt(plan.getPlannedStartAt())
                .plannedEndAt(legs.isEmpty() ? null : legs.get(0).getTrip().getPlannedEndAt())
                .departedAt(plan.getDepartedAt())
                .readyDriverCount((int) legs.stream().filter(leg -> leg.getReadinessConfirmedAt() != null).count())
                .totalDriverCount(legs.size())
                .legs(legs.stream().map(this::toLegResponse).toList())
                .build();
    }

    private SplitDeliveryLegResponse toLegResponse(SplitDeliveryLeg leg) {
        return SplitDeliveryLegResponse.builder()
                .id(leg.getId())
                .tripId(leg.getTrip().getId())
                .vehicleId(leg.getVehicle().getId())
                .driverId(leg.getDriver().getId())
                .stopOrder(leg.getStopOrder())
                .status(leg.getStatus())
                .readinessConfirmedAt(leg.getReadinessConfirmedAt())
                .departedAt(leg.getDepartedAt())
                .dealerArrivedAt(leg.getDealerArrivedAt())
                .handoverConfirmedAt(leg.getHandoverConfirmedAt())
                .failureReportedAt(leg.getFailureReportedAt())
                .failureReason(leg.getFailureReason())
                .build();
    }

    private SplitDeliveryLeg assignedInTransitLeg(SplitDeliveryPlan plan, Long legId, User actor) {
        if (plan.getStatus() != SplitDeliveryPlanStatus.IN_TRANSIT) {
            throw rule("SPLIT_DELIVERY_INCOMPLETE", "Split delivery plan must be IN_TRANSIT");
        }
        SplitDeliveryLeg leg = splitLegRepository.findById(legId)
                .filter(item -> Objects.equals(item.getSplitPlan().getId(), plan.getId()))
                .orElseThrow(() -> notFound("Split delivery leg not found"));
        if (!Objects.equals(leg.getDriver().getUser().getId(), actor.getId())) {
            throw new OutboundDeliveryException("TRIP_DRIVER_SCOPE_FORBIDDEN", HttpStatus.FORBIDDEN,
                    "Authenticated driver is not assigned to this split leg");
        }
        if (leg.getStatus() != SplitDeliveryPlanStatus.IN_TRANSIT) {
            throw rule("SPLIT_DELIVERY_INCOMPLETE", "Split delivery leg must be IN_TRANSIT");
        }
        return leg;
    }

    private List<SplitDeliveryLeg> activeLegs(SplitDeliveryPlan plan) {
        return splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId()).stream()
                .filter(leg -> leg.getStatus() != SplitDeliveryPlanStatus.CANCELLED)
                .toList();
    }

    private SplitLegMilestoneResponse milestoneResponse(SplitDeliveryPlan plan, SplitDeliveryLeg leg) {
        List<SplitDeliveryLeg> legs = activeLegs(plan);
        boolean allArrived = !legs.isEmpty() && legs.stream().allMatch(item -> item.getDealerArrivedAt() != null);
        boolean allHandedOver = allArrived
                && legs.stream().allMatch(item -> item.getHandoverConfirmedAt() != null);
        return SplitLegMilestoneResponse.builder()
                .splitPlanId(plan.getId())
                .legId(leg.getId())
                .status(leg.getStatus())
                .dealerArrivedAt(leg.getDealerArrivedAt())
                .handoverConfirmedAt(leg.getHandoverConfirmedAt())
                .failureReportedAt(leg.getFailureReportedAt())
                .allLegsArrived(allArrived)
                .allLegsHandedOver(allHandedOver)
                .leadPodOtpEnabled(allHandedOver && plan.getStatus() == SplitDeliveryPlanStatus.IN_TRANSIT)
                .build();
    }

    private void auditLeg(User actor, AuditAction action, SplitDeliveryPlan plan, SplitDeliveryLeg leg,
            Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("splitPlanId", plan.getId());
        values.put("legId", leg.getId());
        values.put("driverId", leg.getDriver().getId());
        if (after != null) {
            values.put("state", after);
        }
        auditLogService.log(actor, action, "SPLIT_DELIVERY_LEG", leg.getId(), plan.getPlanNumber(),
                plan.getWarehouse().getId(), before, values);
    }

    private void audit(User actor, AuditAction action, SplitDeliveryPlan plan,
            Map<String, Object> before, Map<String, Object> after) {
        auditLogService.log(actor, action, "SPLIT_DELIVERY_PLAN", plan.getId(), plan.getPlanNumber(),
                plan.getWarehouse().getId(), before, after);
    }

    private Map<String, Object> snapshotWithLegs(SplitDeliveryPlan plan) {
        Map<String, Object> values = snapshot(plan);
        values.put("legs", splitLegRepository.findBySplitPlanIdOrderByStopOrderAsc(plan.getId()).stream()
                .map(leg -> Map.of(
                        "id", leg.getId(),
                        "tripId", leg.getTrip().getId(),
                        "vehicleId", leg.getVehicle().getId(),
                        "driverId", leg.getDriver().getId(),
                        "status", leg.getStatus()))
                .toList());
        return values;
    }

    private Map<String, Object> snapshot(SplitDeliveryPlan plan) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", plan.getId());
        values.put("planNumber", plan.getPlanNumber());
        values.put("doId", plan.getDeliveryOrder().getId());
        values.put("warehouseId", plan.getWarehouse().getId());
        values.put("leadDriverId", plan.getLeadDriver().getId());
        values.put("status", plan.getStatus());
        return values;
    }

    private String generatePlanNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String candidate = "SDP-" + date + "-" + String.format("%04d", sequence);
            if (!splitPlanRepository.existsByPlanNumber(candidate)) {
                return candidate;
            }
        }
        throw rule("SPLIT_PLAN_NUMBER_EXHAUSTED", "Cannot generate split delivery plan number for today");
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

    private OutboundDeliveryException rule(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private record Capacity(BigDecimal weight, BigDecimal volume) {
    }

    private record Allocation(Map<Long, BigDecimal> totalsByItem) {
    }
}
