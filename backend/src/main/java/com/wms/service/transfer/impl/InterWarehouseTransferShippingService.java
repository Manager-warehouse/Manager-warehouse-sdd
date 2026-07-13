package com.wms.service.transfer.impl;

import com.wms.dto.request.InterWarehouseTransferTripAssignRequest;
import com.wms.dto.request.OutboundQcRequest;
import com.wms.dto.request.LoadHandoverRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterWarehouseTransferShippingService {

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final InterWarehouseTransferHelper helper;

    @Transactional
    public InterWarehouseTransferResponse assignTrip(Long id, InterWarehouseTransferTripAssignRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);

        Trip oldTrip = transfer.getTrip();
        if (oldTrip != null) {
            if (oldTrip.getStatus() != TripStatus.PLANNED) {
                throw new BusinessRuleViolationException("TRIP_ALREADY_DEPARTED");
            }
        }

        validateTripSchedule(request);
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.driverId()));
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());

        Long excludedTripId = oldTrip != null ? oldTrip.getId() : null;
        ensureVehicleAndDriverSchedulable(vehicle, driver, request.plannedStartAt(), request.plannedEndAt(), excludedTripId);
        ensureVehicleBelongsToSourceWarehouse(transfer, vehicle);
        ensureDriverBelongsToSourceWarehouse(transfer, driver);

        // T032: Calculate total weight/volume from transfer items
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            BigDecimal qty = item.getPlannedQty() != null ? item.getPlannedQty() : BigDecimal.ZERO;
            BigDecimal weight = item.getProduct().getWeightKg() != null ? item.getProduct().getWeightKg() : BigDecimal.ZERO;
            BigDecimal volume = item.getProduct().getVolumeM3() != null ? item.getProduct().getVolumeM3() : BigDecimal.ZERO;
            totalWeight = totalWeight.add(qty.multiply(weight));
            totalVolume = totalVolume.add(qty.multiply(volume));
        }

        // T033: Reject TRIP_CAPACITY_EXCEEDED when weight/volume exceeds capacity
        if (vehicle.getMaxWeightKg() != null && totalWeight.compareTo(vehicle.getMaxWeightKg()) > 0) {
            throw new BusinessRuleViolationException("TRIP_CAPACITY_EXCEEDED");
        }
        if (vehicle.getMaxVolumeM3() != null && totalVolume.compareTo(vehicle.getMaxVolumeM3()) > 0) {
            throw new BusinessRuleViolationException("TRIP_CAPACITY_EXCEEDED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        Trip trip;
        if (oldTrip != null) {
            // T036: Guard old trip resource release (if no other transfers use it, we reuse/update it)
            long siblingTransfers = transferRepository.countByTripIdAndIdNot(oldTrip.getId(), transfer.getId());
            if (siblingTransfers > 0) {
                // Shared trip. Decouple this transfer from it and create a new trip.
                trip = new Trip();
                trip.setTripNumber(helper.generateTripNumber());
                trip.setCreatedAt(OffsetDateTime.now());
            } else {
                // Only used by this transfer. We can safely update it.
                trip = oldTrip;
            }
        } else {
            trip = new Trip();
            trip.setTripNumber(helper.generateTripNumber());
            trip.setCreatedAt(OffsetDateTime.now());
        }

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDispatcher(actor);
        trip.setPlannedDate(request.plannedStartAt().toLocalDate());
        trip.setPlannedStartAt(request.plannedStartAt());
        trip.setPlannedEndAt(request.plannedEndAt());
        trip.setTripType(TripType.TRANSFER);
        trip.setStatus(TripStatus.PLANNED);
        trip.setWarehouse(transfer.getSourceWarehouse());
        trip.setTotalWeightKg(totalWeight);
        trip.setTotalVolumeM3(totalVolume);
        trip.setCalculatedWeightKg(totalWeight);
        trip.setCalculatedVolumeM3(totalVolume);
        trip.setUpdatedAt(OffsetDateTime.now());

        transfer.setTrip(tripRepository.save(trip));
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_TRIP_ASSIGN, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse shipTransfer(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureSingleTransferTrip(transfer);
        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_NOT_PASSED");
        }
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.setSentQty(item.getPlannedQty());
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_SHIP, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse unshipTransfer(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = helper.snapshot(transfer);
        for (InterWarehouseTransferItem item : helper.items(transfer)) {
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_UNSHIP, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse recordOutboundQc(Long id, OutboundQcRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setOutboundQcPassed(request.passed());
        transfer.setOutboundQcNote(request.note());
        transfer.setOutboundQcPhotoRef(request.photoRef());
        transfer.setOutboundQcBy(actor);
        transfer.setOutboundQcAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_OUTBOUND_QC, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse loadHandover(Long id, LoadHandoverRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());

        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setLoadHandoverPhotoRef(request.photoRef());
        transfer.setLoadHandoverBy(actor);
        transfer.setLoadHandoverAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_LOAD_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse departTransfer(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.APPROVED);
        ensureAssignedDriver(transfer, actor);
        ensureAllSent(transfer);

        if (transfer.getOutboundQcPassed() == null || !transfer.getOutboundQcPassed()) {
            throw new BusinessRuleViolationException("OUTBOUND_QC_NOT_PASSED");
        }
        if (transfer.getLoadHandoverPhotoRef() == null) {
            throw new BusinessRuleViolationException("LOAD_HANDOVER_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        moveSourceToTransit(transfer);
        transfer.getTrip().getDriver().setStatus(DriverStatus.ON_TRIP);
        transfer.getTrip().getVehicle().setStatus(VehicleStatus.ON_TRIP);
        transfer.getTrip().setStatus(TripStatus.IN_TRANSIT);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        transfer.setStatus(InterWarehouseTransferStatus.IN_TRANSIT);
        transfer.setUpdatedAt(OffsetDateTime.now());
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_DEPART, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    private void validateTripSchedule(InterWarehouseTransferTripAssignRequest request) {
        if (!request.plannedEndAt().isAfter(request.plannedStartAt())) {
            throw new BusinessRuleViolationException("TRIP_SCHEDULE_INVALID");
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.plannedStartAt().isBefore(now.minusMinutes(15))) {
            throw new BusinessRuleViolationException("TRIP_START_IN_PAST");
        }
        if (request.plannedEndAt().isBefore(now)) {
            throw new BusinessRuleViolationException("TRIP_END_IN_PAST");
        }
    }

    private void ensureVehicleAndDriverSchedulable(Vehicle vehicle,
                                                   Driver driver,
                                                   LocalDateTime plannedStartAt,
                                                   LocalDateTime plannedEndAt,
                                                   Long excludedTripId) {
        if (Boolean.FALSE.equals(vehicle.getIsActive()) || vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessRuleViolationException("VEHICLE_NOT_AVAILABLE");
        }
        if (Boolean.FALSE.equals(driver.getIsActive()) || driver.getStatus() == DriverStatus.UNAVAILABLE) {
            throw new BusinessRuleViolationException("DRIVER_NOT_AVAILABLE");
        }
        if (tripRepository.existsVehicleScheduleOverlapExcludingTrip(vehicle.getId(), plannedStartAt, plannedEndAt, InterWarehouseTransferHelper.RESOURCE_BLOCKING_TRIP_STATUSES, excludedTripId)
                || tripRepository.existsDriverScheduleOverlapExcludingTrip(driver.getId(), plannedStartAt, plannedEndAt, InterWarehouseTransferHelper.RESOURCE_BLOCKING_TRIP_STATUSES, excludedTripId)) {
            throw new BusinessRuleViolationException("TRIP_RESOURCE_OVERLAP");
        }
    }

    private void ensureVehicleBelongsToSourceWarehouse(InterWarehouseTransfer transfer, Vehicle vehicle) {
        if (vehicle.getWarehouse() == null || !Objects.equals(vehicle.getWarehouse().getId(), transfer.getSourceWarehouse().getId())) {
            throw new BusinessRuleViolationException("VEHICLE_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureDriverBelongsToSourceWarehouse(InterWarehouseTransfer transfer, Driver driver) {
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long driverUserId = driver.getUser().getId();
        if (!assignmentRepository.findWarehouseIdsByUserId(driverUserId).contains(sourceWarehouseId)) {
            throw new BusinessRuleViolationException("DRIVER_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureSingleTransferTrip(InterWarehouseTransfer transfer) {
        if (transfer.getTrip() == null || transfer.getTrip().getTripType() != TripType.TRANSFER) {
            throw new BusinessRuleViolationException("TRANSFER_TRIP_REQUIRED");
        }
    }

    private void ensureAssignedDriver(InterWarehouseTransfer transfer, User actor) {
        ensureSingleTransferTrip(transfer);
        Long driverUserId = transfer.getTrip().getDriver().getUser().getId();
        if (!Objects.equals(driverUserId, actor.getId())) {
            throw new BusinessRuleViolationException("ASSIGNED_DRIVER_REQUIRED");
        }
    }

    private void ensureAllSent(InterWarehouseTransfer transfer) {
        if (helper.items(transfer).stream().anyMatch(item -> item.getSentQty() == null
                || item.getSentQty().compareTo(item.getPlannedQty()) != 0)) {
            throw new BusinessRuleViolationException("SENT_QTY_REQUIRED");
        }
    }

    private void moveSourceToTransit(InterWarehouseTransfer transfer) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(InterWarehouseTransferHelper.IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation transitLocation = locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(
                        transitWarehouse.getId(), LocationType.BIN)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_LOCATION_NOT_CONFIGURED"));
        for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            Inventory source = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            source.setTotalQty(source.getTotalQty().subtract(allocation.getAllocatedQty()));
            BigDecimal newReserved = source.getReservedQty().subtract(allocation.getAllocatedQty());
            if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleViolationException("INVENTORY_INVARIANT_VIOLATED: Reserved quantity cannot be negative");
            }
            source.setReservedQty(newReserved);
            source.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(source);
            helper.upsertInventory(transitWarehouse, allocation.getTransferItem().getProduct(), source.getBatch(),
                    transitLocation, allocation.getAllocatedQty(), source.getCostPrice());
        }
    }

    @Transactional
    public InterWarehouseTransferResponse driverArrive(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        ensureAssignedDriver(transfer, actor);

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setDriverArrivedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_ARRIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse receivingHandover(Long id, LoadHandoverRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);

        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        helper.ensureWarehouseScope(actor, targetWarehouseId);

        if (transfer.getDriverArrivedAt() == null) {
            throw new BusinessRuleViolationException("DRIVER_ARRIVE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setArrivalHandoverAt(OffsetDateTime.now());
        transfer.setArrivalHandoverBy(actor);
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_ARRIVAL_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnDepart(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        ensureAssignedDriver(transfer, actor);

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnDepartedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_DEPART, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnArrive(Long id, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        ensureAssignedDriver(transfer, actor);
        if (transfer.getReturnDepartedAt() == null) {
            throw new BusinessRuleViolationException("RETURN_DEPART_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnArrivedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_ARRIVE, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }

    @Transactional
    public InterWarehouseTransferResponse returnHandover(Long id, LoadHandoverRequest request, User actor) {
        InterWarehouseTransfer transfer = helper.findTransfer(id);
        helper.requireStatus(transfer, InterWarehouseTransferStatus.IN_TRANSIT);
        if (!Boolean.TRUE.equals(transfer.isReturned())) {
            throw new BusinessRuleViolationException("TRANSFER_NOT_RETURNED_LEG");
        }
        helper.ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        if (transfer.getReturnArrivedAt() == null) {
            throw new BusinessRuleViolationException("RETURN_ARRIVE_REQUIRED");
        }

        Map<String, Object> before = helper.snapshot(transfer);
        transfer.setReturnArrivalHandoverAt(OffsetDateTime.now());
        transfer.setReturnArrivalHandoverBy(actor);
        transfer.setReturnPhotoRef(request.photoRef());
        transfer.setUpdatedAt(OffsetDateTime.now());

        InterWarehouseTransfer saved = transferRepository.save(transfer);
        helper.audit(saved, actor, AuditAction.TRANSFER_RETURN_HANDOVER, before, helper.snapshot(saved));
        return helper.toResponse(saved);
    }
}
