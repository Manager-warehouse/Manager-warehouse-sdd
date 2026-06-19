package com.wms.service.impl;

import com.wms.dto.request.*;
import com.wms.dto.response.TransferItemResponse;
import com.wms.dto.response.TransferResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.mapper.TransferMapper;
import com.wms.service.TransferService;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final String ENTITY = "TRANSFER";
    private static final String IN_TRANSIT_WAREHOUSE_CODE = "IN_TRANSIT";
    private static final List<TransferStatus> DUPLICATE_IGNORED_STATUSES =
            List.of(TransferStatus.REJECTED, TransferStatus.CANCELLED);
    private static final List<TripStatus> RESOURCE_BLOCKING_TRIP_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.IN_TRANSIT);

    private final TransferRepository transferRepository;
    private final TransferItemRepository transferItemRepository;
    private final TransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final TransferMapper transferMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getAllTransfers(User actor) {
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(transfer -> canViewTransfer(actor, transfer))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(Long id, User actor) {
        Transfer transfer = findTransfer(id);
        if (!canViewTransfer(actor, transfer)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
        return toResponse(transfer);
    }

    private boolean canViewTransfer(User actor, Transfer transfer) {
        if (actor.getRole() == UserRole.ADMIN
                || actor.getRole() == UserRole.CEO
                || actor.getRole() == UserRole.PLANNER) {
            return true;
        }
        if (actor.getRole() == UserRole.DRIVER) {
            return transfer.getTrip() != null
                    && transfer.getTrip().getDriver() != null
                    && transfer.getTrip().getDriver().getUser() != null
                    && Objects.equals(transfer.getTrip().getDriver().getUser().getId(), actor.getId());
        }

        List<Long> warehouseIds = assignmentRepository.findWarehouseIdsByUserId(actor.getId());
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long destinationWarehouseId = transfer.getDestinationWarehouse().getId();
        return switch (actor.getRole()) {
            case DISPATCHER -> warehouseIds.contains(sourceWarehouseId);
            case WAREHOUSE_STAFF -> warehouseIds.contains(destinationWarehouseId);
            case STOREKEEPER, WAREHOUSE_MANAGER ->
                    warehouseIds.contains(sourceWarehouseId) || warehouseIds.contains(destinationWarehouseId);
            default -> false;
        };
    }

    @Override
    @Transactional
    public TransferResponse createTransfer(TransferCreateRequest request, User actor) {
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureUniqueExternalInstruction(request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), null);

        OffsetDateTime now = OffsetDateTime.now();
        Transfer transfer = new Transfer();
        transfer.setTransferNumber(generateTransferNumber());
        applyTransferFields(transfer, request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), request.plannedDate(), request.notes());
        transfer.setStatus(TransferStatus.NEW);
        transfer.setCreatedBy(actor);
        transfer.setCreatedAt(now);
        transfer.setUpdatedAt(now);

        Transfer saved = transferRepository.save(transfer);
        replaceItems(saved, request.items());
        audit(saved, actor, AuditAction.CREATE, Map.of(), snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse updateTransfer(Long id, TransferUpdateRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.NEW);
        ensureDifferentWarehouses(request.sourceWarehouseId(), request.destinationWarehouseId());
        ensureUniqueExternalInstruction(request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), id);
        Map<String, Object> before = snapshot(transfer);

        applyTransferFields(transfer, request.externalInstructionCode(), request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.documentDate(), request.plannedDate(), request.notes());
        transfer.setUpdatedAt(OffsetDateTime.now());
        replaceItems(transfer, request.items());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.UPDATE, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse cancelTransfer(Long id, TransferReasonRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        Map<String, Object> before = snapshot(transfer);
        if (transfer.getStatus() == TransferStatus.APPROVED) {
            ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
            ensureNotLoaded(transfer);
            releaseReservations(transfer);
        } else if (transfer.getStatus() != TransferStatus.NEW) {
            throw new BusinessRuleViolationException("TRANSFER_CANCEL_NOT_ALLOWED");
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        transfer.setRejectionReason(requiredReason(request, "CANCEL_REASON_REQUIRED"));
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_CANCEL, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse approveTransfer(Long id, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.NEW);
        ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = snapshot(transfer);
        allocateReservations(transfer);
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(actor);
        transfer.setApprovedAt(OffsetDateTime.now());
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_APPROVE, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse rejectTransfer(Long id, TransferReasonRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.NEW);
        ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = snapshot(transfer);
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectedBy(actor);
        transfer.setRejectedAt(OffsetDateTime.now());
        transfer.setRejectionReason(requiredReason(request, "REJECTION_REASON_REQUIRED"));
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_REJECT, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse assignTrip(Long id, TransferTripAssignRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.APPROVED);
        if (transfer.getTrip() != null) {
            throw new BusinessRuleViolationException("TRANSFER_ALREADY_HAS_TRIP");
        }
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.vehicleId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.driverId()));
        ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureVehicleAndDriverAvailable(vehicle, driver, request.plannedDate());
        ensureVehicleBelongsToSourceWarehouse(transfer, vehicle);
        ensureDriverBelongsToSourceWarehouse(transfer, driver);

        Map<String, Object> before = snapshot(transfer);
        Trip trip = new Trip();
        trip.setTripNumber(generateTripNumber());
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDispatcher(actor);
        trip.setPlannedDate(request.plannedDate());
        trip.setTripType(TripType.TRANSFER);
        trip.setStatus(TripStatus.PLANNED);
        trip.setTotalWeightKg(BigDecimal.ZERO);
        trip.setTotalVolumeM3(BigDecimal.ZERO);
        trip.setCreatedAt(OffsetDateTime.now());
        trip.setUpdatedAt(OffsetDateTime.now());
        transfer.setTrip(tripRepository.save(trip));
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_TRIP_ASSIGN, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse shipTransfer(Long id, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.APPROVED);
        ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        ensureSingleTransferTrip(transfer);
        Map<String, Object> before = snapshot(transfer);
        for (TransferItem item : items(transfer)) {
            item.setSentQty(item.getPlannedQty());
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_SHIP, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse unshipTransfer(Long id, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.APPROVED);
        ensureWarehouseScope(actor, transfer.getSourceWarehouse().getId());
        Map<String, Object> before = snapshot(transfer);
        for (TransferItem item : items(transfer)) {
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_UNSHIP, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse departTransfer(Long id, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.APPROVED);
        ensureAssignedDriver(transfer, actor);
        ensureAllSent(transfer);
        Map<String, Object> before = snapshot(transfer);
        moveSourceToTransit(transfer);
        transfer.getTrip().setStatus(TripStatus.IN_TRANSIT);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_DEPART, before, snapshot(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TransferResponse receiveCount(Long id, TransferReceiveCountRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.IN_TRANSIT);
        ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        Map<Long, TransferItem> itemById = itemMap(transfer);
        Map<String, Object> before = snapshot(transfer);
        for (TransferReceiveCountItemRequest line : request.items()) {
            TransferItem item = requireItem(itemById, line.transferItemId());
            if (line.receivedQty().compareTo(item.getSentQty()) > 0) {
                throw new BusinessRuleViolationException("OVER_RECEIPT_BLOCKED");
            }
            if (line.receivedQty().compareTo(item.getSentQty()) != 0 && isBlank(line.issueReason())) {
                throw new BusinessRuleViolationException("ISSUE_REASON_REQUIRED");
            }
            item.setWorkerReceivedQty(line.receivedQty());
            item.setIssueReason(line.issueReason());
            transferItemRepository.save(item);
        }
        audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_COUNT, before, snapshot(transfer));
        return toResponse(transfer);
    }

    @Override
    @Transactional
    public TransferResponse receiveCheck(Long id, TransferReceiveCheckRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.IN_TRANSIT);
        ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        Map<Long, TransferItem> itemById = itemMap(transfer);
        Map<String, Object> before = snapshot(transfer);
        for (TransferReceiveCheckItemRequest line : request.items()) {
            TransferItem item = requireItem(itemById, line.transferItemId());
            validateReceiveCheckLine(transfer, item, line);
            item.setReceivedQty(line.confirmedQty());
            item.setQcPassedQty(line.qcPassedQty());
            item.setQcFailedQty(line.qcFailedQty());
            item.setDestinationLocation(line.destinationLocationId() == null
                    ? null
                    : reference(WarehouseLocation.class, line.destinationLocationId()));
            item.setCheckerNote(line.checkerNote());
            item.setQcFailureReason(line.qcFailureReason());
            item.setCheckedBy(actor);
            item.setCheckedAt(OffsetDateTime.now());
            item.setVarianceQty(line.confirmedQty().subtract(item.getSentQty()));
            transferItemRepository.save(item);
        }
        audit(transfer, actor, AuditAction.TRANSFER_RECEIVE_CHECK, before, snapshot(transfer));
        return toResponse(transfer);
    }

    @Override
    @Transactional
    public TransferResponse finalReceive(Long id, TransferFinalReceiveRequest request, User actor) {
        Transfer transfer = findTransfer(id);
        requireStatus(transfer, TransferStatus.IN_TRANSIT);
        ensureWarehouseScope(actor, transfer.getDestinationWarehouse().getId());
        ensureAllChecked(transfer);
        boolean shortage = items(transfer).stream().anyMatch(item -> item.getVarianceQty().compareTo(BigDecimal.ZERO) < 0);
        if (shortage && isBlank(request.discrepancyReason())) {
            throw new BusinessRuleViolationException("DISCREPANCY_REASON_REQUIRED");
        }
        Map<String, Object> before = snapshot(transfer);
        moveTransitToDestination(transfer);
        transfer.setStatus(shortage ? TransferStatus.COMPLETED_WITH_DISCREPANCY : TransferStatus.COMPLETED);
        transfer.setDiscrepancyReason(request.discrepancyReason());
        transfer.setConfirmedBy(actor);
        transfer.setConfirmedAt(OffsetDateTime.now());
        transfer.setActualReceivedDate(OffsetDateTime.now().toLocalDate());
        transfer.setUpdatedAt(OffsetDateTime.now());
        transfer.getTrip().setStatus(TripStatus.COMPLETED);
        transfer.getTrip().setUpdatedAt(OffsetDateTime.now());
        Transfer saved = transferRepository.save(transfer);
        audit(saved, actor, AuditAction.TRANSFER_FINAL_RECEIVE, before, snapshot(saved));
        return toResponse(saved);
    }

    private void applyTransferFields(Transfer transfer, String externalInstructionCode, Long sourceWarehouseId,
                                     Long destinationWarehouseId, java.time.LocalDate documentDate,
                                     java.time.LocalDate plannedDate, String notes) {
        transfer.setExternalInstructionCode(externalInstructionCode.trim());
        transfer.setSourceWarehouse(reference(Warehouse.class, sourceWarehouseId));
        transfer.setDestinationWarehouse(reference(Warehouse.class, destinationWarehouseId));
        transfer.setDocumentDate(documentDate);
        transfer.setPlannedDate(plannedDate);
        transfer.setNotes(notes);
    }

    private void replaceItems(Transfer transfer, List<TransferItemRequest> requests) {
        transferItemRepository.deleteByTransferId(transfer.getId());
        for (TransferItemRequest request : requests) {
            TransferItem item = new TransferItem();
            item.setTransfer(transfer);
            item.setProduct(reference(Product.class, request.productId()));
            item.setSourceLocation(request.sourceLocationId() == null ? null : reference(WarehouseLocation.class, request.sourceLocationId()));
            item.setDestinationLocation(request.destinationLocationId() == null ? null : reference(WarehouseLocation.class, request.destinationLocationId()));
            item.setPlannedQty(request.plannedQty());
            transferItemRepository.save(item);
        }
    }

    private void allocateReservations(Transfer transfer) {
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
        for (TransferItem item : items(transfer)) {
            BigDecimal remaining = item.getPlannedQty();
            for (Inventory inventory : inventoryRepository.findReservableForUpdate(
                    transfer.getSourceWarehouse().getId(), item.getProduct().getId())) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal available = inventory.getTotalQty().subtract(inventory.getReservedQty());
                BigDecimal allocated = available.min(remaining);
                inventory.setReservedQty(inventory.getReservedQty().add(allocated));
                inventory.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inventory);
                allocationRepository.save(TransferAllocation.builder()
                        .transferItem(item)
                        .inventory(inventory)
                        .allocatedQty(allocated)
                        .build());
                remaining = remaining.subtract(allocated);
            }
            if (remaining.signum() > 0) {
                BigDecimal available = item.getPlannedQty().subtract(remaining);
                throw new BusinessRuleViolationException(
                        "INSUFFICIENT_AVAILABLE_STOCK: "
                                + item.getProduct().getSku()
                                + " required " + item.getPlannedQty()
                                + ", available " + available
                                + " in " + transfer.getSourceWarehouse().getCode());
            }
        }
    }

    private void releaseReservations(Transfer transfer) {
        for (TransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            inventory.setReservedQty(inventory.getReservedQty().subtract(allocation.getAllocatedQty()));
            inventory.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inventory);
        }
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
    }

    private void moveSourceToTransit(Transfer transfer) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation transitLocation = locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(
                        transitWarehouse.getId(), LocationType.BIN)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_LOCATION_NOT_CONFIGURED"));
        for (TransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            Inventory source = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            source.setTotalQty(source.getTotalQty().subtract(allocation.getAllocatedQty()));
            source.setReservedQty(source.getReservedQty().subtract(allocation.getAllocatedQty()));
            source.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(source);
            upsertInventory(transitWarehouse, allocation.getTransferItem().getProduct(), source.getBatch(),
                    transitLocation, allocation.getAllocatedQty(), source.getCostPrice());
        }
    }

    private void moveTransitToDestination(Transfer transfer) {
        Warehouse transitWarehouse = warehouseRepository.findByCode(IN_TRANSIT_WAREHOUSE_CODE)
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_WAREHOUSE_NOT_CONFIGURED"));
        WarehouseLocation quarantineLocation = null;
        for (TransferItem item : items(transfer)) {
            BigDecimal remainingPassed = zero(item.getQcPassedQty());
            BigDecimal remainingFailed = zero(item.getQcFailedQty());
            for (TransferAllocation allocation : allocationRepository.findByTransferItemId(item.getId())) {
                Inventory transit = inventoryRepository.findByStockKeyForUpdate(transitWarehouse.getId(),
                                item.getProduct().getId(), allocation.getInventory().getBatch().getId(),
                                firstTransitLocation(transitWarehouse).getId())
                        .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_STOCK_NOT_FOUND"));
                BigDecimal qty = allocation.getAllocatedQty();
                transit.setTotalQty(transit.getTotalQty().subtract(qty));
                transit.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(transit);
                BigDecimal passQty = qty.min(remainingPassed);
                if (passQty.signum() > 0) {
                    upsertInventory(transfer.getDestinationWarehouse(), item.getProduct(), transit.getBatch(),
                            item.getDestinationLocation(), passQty, transit.getCostPrice());
                    remainingPassed = remainingPassed.subtract(passQty);
                }
                BigDecimal failQty = qty.subtract(passQty).min(remainingFailed);
                if (failQty.signum() > 0) {
                    if (quarantineLocation == null) {
                        quarantineLocation = findQuarantineLocation(transfer);
                    }
                    upsertInventory(transfer.getDestinationWarehouse(), item.getProduct(), transit.getBatch(),
                            quarantineLocation, failQty, transit.getCostPrice());
                    remainingFailed = remainingFailed.subtract(failQty);
                }
            }
        }
    }

    private void upsertInventory(Warehouse warehouse, Product product, Batch batch, WarehouseLocation location,
                                 BigDecimal qty, BigDecimal costPrice) {
        Inventory inventory = inventoryRepository.findByStockKeyForUpdate(
                        warehouse.getId(), product.getId(), batch.getId(), location.getId())
                .orElseGet(() -> Inventory.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .batch(batch)
                        .location(location)
                        .totalQty(BigDecimal.ZERO)
                        .reservedQty(BigDecimal.ZERO)
                        .costPrice(costPrice)
                        .version(0)
                        .updatedAt(OffsetDateTime.now())
                        .build());
        inventory.setTotalQty(inventory.getTotalQty().add(qty));
        inventory.setUpdatedAt(OffsetDateTime.now());
        inventoryRepository.save(inventory);
    }

    private void validateReceiveCheckLine(Transfer transfer, TransferItem item, TransferReceiveCheckItemRequest line) {
        if (item.getWorkerReceivedQty() == null) {
            throw new BusinessRuleViolationException("WORKER_COUNT_REQUIRED");
        }
        if (line.confirmedQty().compareTo(item.getSentQty()) > 0) {
            throw new BusinessRuleViolationException("OVER_RECEIPT_BLOCKED");
        }
        if (line.confirmedQty().compareTo(item.getWorkerReceivedQty()) != 0 && isBlank(line.checkerNote())) {
            throw new BusinessRuleViolationException("CHECKER_NOTE_REQUIRED");
        }
        if (line.qcPassedQty().add(line.qcFailedQty()).compareTo(line.confirmedQty()) != 0) {
            throw new BusinessRuleViolationException("QC_TOTAL_MUST_MATCH_CONFIRMED_QTY");
        }
        if (line.qcFailedQty().signum() > 0 && isBlank(line.qcFailureReason())) {
            throw new BusinessRuleViolationException("QC_FAILURE_REASON_REQUIRED");
        }
        if (line.qcPassedQty().signum() > 0) {
            if (line.destinationLocationId() == null) {
                throw new BusinessRuleViolationException("DESTINATION_LOCATION_REQUIRED");
            }
            WarehouseLocation destination = locationRepository.findById(line.destinationLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination location not found: " + line.destinationLocationId()));
            if (!Objects.equals(destination.getWarehouse().getId(), transfer.getDestinationWarehouse().getId())
                    || Boolean.FALSE.equals(destination.getIsActive())) {
                throw new BusinessRuleViolationException("INVALID_DESTINATION_LOCATION");
            }
        }
    }

    private WarehouseLocation findQuarantineLocation(Transfer transfer) {
        return locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(
                        transfer.getDestinationWarehouse().getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED"));
    }

    private WarehouseLocation firstTransitLocation(Warehouse transitWarehouse) {
        return locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(transitWarehouse.getId(), LocationType.BIN)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_LOCATION_NOT_CONFIGURED"));
    }

    private void ensureAllSent(Transfer transfer) {
        if (items(transfer).stream().anyMatch(item -> item.getSentQty() == null
                || item.getSentQty().compareTo(item.getPlannedQty()) != 0)) {
            throw new BusinessRuleViolationException("SENT_QTY_REQUIRED");
        }
    }

    private void ensureAllChecked(Transfer transfer) {
        if (items(transfer).stream().anyMatch(item -> item.getReceivedQty() == null
                || item.getQcPassedQty() == null || item.getQcFailedQty() == null)) {
            throw new BusinessRuleViolationException("RECEIVE_CHECK_REQUIRED");
        }
    }

    private void ensureNotLoaded(Transfer transfer) {
        if (items(transfer).stream().anyMatch(item -> item.getSentQty() != null)) {
            throw new BusinessRuleViolationException("UNSHIP_REQUIRED_BEFORE_CANCEL");
        }
    }

    private void ensureSingleTransferTrip(Transfer transfer) {
        if (transfer.getTrip() == null || transfer.getTrip().getTripType() != TripType.TRANSFER) {
            throw new BusinessRuleViolationException("TRANSFER_TRIP_REQUIRED");
        }
    }

    private void ensureAssignedDriver(Transfer transfer, User actor) {
        ensureSingleTransferTrip(transfer);
        Long driverUserId = transfer.getTrip().getDriver().getUser().getId();
        if (!Objects.equals(driverUserId, actor.getId())) {
            throw new BusinessRuleViolationException("ASSIGNED_DRIVER_REQUIRED");
        }
    }

    private void ensureVehicleAndDriverAvailable(Vehicle vehicle, Driver driver, java.time.LocalDate plannedDate) {
        if (Boolean.FALSE.equals(vehicle.getIsActive()) || vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("VEHICLE_NOT_AVAILABLE");
        }
        if (Boolean.FALSE.equals(driver.getIsActive()) || driver.getStatus() != DriverStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("DRIVER_NOT_AVAILABLE");
        }
        if (tripRepository.existsByVehicleIdAndPlannedDateAndStatusIn(vehicle.getId(), plannedDate, RESOURCE_BLOCKING_TRIP_STATUSES)
                || tripRepository.existsByDriverIdAndPlannedDateAndStatusIn(driver.getId(), plannedDate, RESOURCE_BLOCKING_TRIP_STATUSES)) {
            throw new BusinessRuleViolationException("TRIP_RESOURCE_OVERLAP");
        }
    }

    private void ensureDriverBelongsToSourceWarehouse(Transfer transfer, Driver driver) {
        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long driverUserId = driver.getUser().getId();
        if (!assignmentRepository.findWarehouseIdsByUserId(driverUserId).contains(sourceWarehouseId)) {
            throw new BusinessRuleViolationException("DRIVER_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureVehicleBelongsToSourceWarehouse(Transfer transfer, Vehicle vehicle) {
        if (vehicle.getWarehouse() == null || !Objects.equals(vehicle.getWarehouse().getId(), transfer.getSourceWarehouse().getId())) {
            throw new BusinessRuleViolationException("VEHICLE_SOURCE_WAREHOUSE_REQUIRED");
        }
    }

    private void ensureWarehouseScope(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        if (!assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    private void ensureDifferentWarehouses(Long sourceWarehouseId, Long destinationWarehouseId) {
        if (Objects.equals(sourceWarehouseId, destinationWarehouseId)) {
            throw new BusinessRuleViolationException("SOURCE_DESTINATION_MUST_DIFFER");
        }
    }

    private void ensureUniqueExternalInstruction(String code, Long sourceWarehouseId, Long destinationWarehouseId,
                                                 java.time.LocalDate documentDate, Long currentId) {
        boolean exists = currentId == null
                ? transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotIn(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, DUPLICATE_IGNORED_STATUSES)
                : transferRepository.existsByExternalInstructionCodeAndSourceWarehouseIdAndDestinationWarehouseIdAndDocumentDateAndStatusNotInAndIdNot(
                    code.trim(), sourceWarehouseId, destinationWarehouseId, documentDate, DUPLICATE_IGNORED_STATUSES, currentId);
        if (exists) {
            throw new BusinessRuleViolationException("DUPLICATE_EXTERNAL_INSTRUCTION");
        }
    }

    private Transfer findTransfer(Long id) {
        return transferRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
    }

    private List<TransferItem> items(Transfer transfer) {
        return transferItemRepository.findByTransferIdOrderById(transfer.getId());
    }

    private Map<Long, TransferItem> itemMap(Transfer transfer) {
        return items(transfer).stream().collect(Collectors.toMap(TransferItem::getId, Function.identity()));
    }

    private TransferItem requireItem(Map<Long, TransferItem> items, Long id) {
        TransferItem item = items.get(id);
        if (item == null) {
            throw new ResourceNotFoundException("Transfer item not found: " + id);
        }
        return item;
    }

    private void requireStatus(Transfer transfer, TransferStatus expected) {
        if (transfer.getStatus() != expected) {
            throw new BusinessRuleViolationException("INVALID_TRANSFER_STATUS");
        }
    }

    private String requiredReason(TransferReasonRequest request, String code) {
        if (request == null || isBlank(request.reason())) {
            throw new BusinessRuleViolationException(code);
        }
        return request.reason();
    }

    private <T> T reference(Class<T> type, Long id) {
        return entityManager.getReference(type, id);
    }

    private TransferResponse toResponse(Transfer transfer) {
        return transferMapper.toResponse(transfer, items(transfer));
    }

    private void audit(Transfer transfer, User actor, AuditAction action,
                       Map<String, Object> before, Map<String, Object> after) {
        auditUtil.logChange(actor, action, ENTITY, transfer.getId(), transfer.getTransferNumber(), before, after);
    }

    private Map<String, Object> snapshot(Transfer transfer) {
        return PartnerAuditUtil.values(
                "transferNumber", transfer.getTransferNumber(),
                "externalInstructionCode", transfer.getExternalInstructionCode(),
                "sourceWarehouseId", transfer.getSourceWarehouse() == null ? null : transfer.getSourceWarehouse().getId(),
                "destinationWarehouseId", transfer.getDestinationWarehouse() == null ? null : transfer.getDestinationWarehouse().getId(),
                "status", transfer.getStatus(),
                "tripId", transfer.getTrip() == null ? null : transfer.getTrip().getId(),
                "documentDate", transfer.getDocumentDate(),
                "plannedDate", transfer.getPlannedDate(),
                "discrepancyReason", transfer.getDiscrepancyReason(),
                "rejectionReason", transfer.getRejectionReason(),
                "notes", transfer.getNotes());
    }

    private String generateTransferNumber() {
        String prefix = "TRF-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (transferRepository.existsByTransferNumber(candidate));
        return candidate;
    }

    private String generateTripNumber() {
        String prefix = "TTR-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (tripRepository.existsByTripNumber(candidate));
        return candidate;
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
