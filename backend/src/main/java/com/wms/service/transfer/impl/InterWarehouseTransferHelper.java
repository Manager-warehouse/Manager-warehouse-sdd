package com.wms.service.transfer.impl;

import com.wms.dto.request.InterWarehouseTransferReasonRequest;
import com.wms.dto.response.InterWarehouseTransferResponse;
import com.wms.entity.*;
import com.wms.enums.LocationType;
import com.wms.enums.InterWarehouseTransferStatus;
import com.wms.enums.AuditAction;
import com.wms.enums.UserRole;
import com.wms.enums.TripStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.mapper.InterWarehouseTransferMapper;
import com.wms.util.PartnerAuditUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterWarehouseTransferHelper {

    public static final String ENTITY = "TRANSFER";
    public static final String IN_TRANSIT_WAREHOUSE_CODE = "IN_TRANSIT";
    public static final List<InterWarehouseTransferStatus> DUPLICATE_IGNORED_STATUSES =
            List.of(InterWarehouseTransferStatus.REJECTED, InterWarehouseTransferStatus.CANCELLED);
    public static final List<TripStatus> RESOURCE_BLOCKING_TRIP_STATUSES =
            List.of(TripStatus.PLANNED, TripStatus.IN_TRANSIT);
    public static final int TRANSFER_MIN_NOTICE_DAYS = 7;
    public static final int TRANSFER_WARNING_WINDOW_DAYS = 3;

    private final InterWarehouseTransferRepository transferRepository;
    private final InterWarehouseTransferItemRepository transferItemRepository;
    private final InterWarehouseTransferAllocationRepository allocationRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository locationRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final TripRepository tripRepository;
    private final InterWarehouseTransferMapper transferMapper;
    private final PartnerAuditUtil auditUtil;
    private final EntityManager entityManager;

    public WarehouseLocation findQuarantineLocation(InterWarehouseTransfer transfer) {
        Long targetWarehouseId = transfer.isReturned() ? transfer.getSourceWarehouse().getId() : transfer.getDestinationWarehouse().getId();
        return locationRepository.findByWarehouseIdAndIsQuarantineTrueAndIsActiveTrue(targetWarehouseId)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("QUARANTINE_LOCATION_NOT_CONFIGURED"));
    }

    public WarehouseLocation firstTransitLocation(Warehouse transitWarehouse) {
        return locationRepository.findByWarehouseIdAndTypeAndIsActiveTrue(transitWarehouse.getId(), LocationType.BIN)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("IN_TRANSIT_LOCATION_NOT_CONFIGURED"));
    }

    public void upsertInventory(Warehouse warehouse, Product product, Batch batch, WarehouseLocation location,
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

    public void allocateReservations(InterWarehouseTransfer transfer) {
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
        for (InterWarehouseTransferItem item : items(transfer)) {
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
                allocationRepository.save(InterWarehouseTransferAllocation.builder()
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

    public void releaseReservations(InterWarehouseTransfer transfer) {
        for (InterWarehouseTransferAllocation allocation : allocationRepository.findByTransferItemTransferId(transfer.getId())) {
            Inventory inventory = inventoryRepository.findByIdForUpdate(allocation.getInventory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + allocation.getInventory().getId()));
            BigDecimal newReserved = inventory.getReservedQty().subtract(allocation.getAllocatedQty());
            if (newReserved.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newReserved = java.math.BigDecimal.ZERO;
            }
            inventory.setReservedQty(newReserved);
            inventory.setUpdatedAt(OffsetDateTime.now());
            inventoryRepository.save(inventory);
        }
        allocationRepository.deleteByTransferItemTransferId(transfer.getId());
    }

    public InterWarehouseTransfer findTransfer(Long id) {
        return transferRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
    }

    public List<InterWarehouseTransferItem> items(InterWarehouseTransfer transfer) {
        return transferItemRepository.findByTransferIdOrderById(transfer.getId());
    }

    public Map<Long, InterWarehouseTransferItem> itemMap(InterWarehouseTransfer transfer) {
        return items(transfer).stream().collect(Collectors.toMap(InterWarehouseTransferItem::getId, Function.identity()));
    }

    public InterWarehouseTransferItem requireItem(Map<Long, InterWarehouseTransferItem> items, Long id) {
        InterWarehouseTransferItem item = items.get(id);
        if (item == null) {
            throw new ResourceNotFoundException("Transfer item not found: " + id);
        }
        return item;
    }

    public void requireStatus(InterWarehouseTransfer transfer, InterWarehouseTransferStatus expected) {
        if (transfer.getStatus() != expected) {
            throw new BusinessRuleViolationException("INVALID_TRANSFER_STATUS");
        }
    }

    public String requiredReason(InterWarehouseTransferReasonRequest request, String code) {
        if (request == null || isBlank(request.reason())) {
            throw new BusinessRuleViolationException(code);
        }
        return request.reason();
    }

    public <T> T reference(Class<T> type, Long id) {
        return entityManager.getReference(type, id);
    }

    public void ensureWarehouseScope(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        if (!assignmentRepository.findWarehouseIdsByUserId(actor.getId()).contains(warehouseId)) {
            throw new BusinessRuleViolationException("WAREHOUSE_SCOPE_REQUIRED");
        }
    }

    /**
     * Load warehouse IDs once per request to avoid N+1 queries when filtering a list.
     * ADMIN and CEO have no warehouse restrictions, so return empty list as sentinel.
     */
    public List<Long> loadWarehouseIds(User actor) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return List.of();
        }
        return assignmentRepository.findWarehouseIdsByUserId(actor.getId());
    }

    /**
     * Overload that accepts pre-loaded warehouse IDs to avoid N+1 when filtering a list.
     */
    public boolean canViewTransfer(User actor, List<Long> warehouseIds, InterWarehouseTransfer transfer) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return true;
        }

        Long sourceWarehouseId = transfer.getSourceWarehouse().getId();
        Long destinationWarehouseId = transfer.getDestinationWarehouse().getId();

        if (actor.getRole() == UserRole.DRIVER) {
            boolean belongsToWarehouse = warehouseIds.contains(sourceWarehouseId) || warehouseIds.contains(destinationWarehouseId);
            return belongsToWarehouse && transfer.getTrip() != null
                    && transfer.getTrip().getDriver() != null
                    && transfer.getTrip().getDriver().getUser() != null
                    && Objects.equals(transfer.getTrip().getDriver().getUser().getId(), actor.getId());
        }

        return switch (actor.getRole()) {
            case DISPATCHER -> warehouseIds.contains(sourceWarehouseId);
            case PLANNER, WAREHOUSE_STAFF, STOREKEEPER, WAREHOUSE_MANAGER ->
                    warehouseIds.contains(sourceWarehouseId) || warehouseIds.contains(destinationWarehouseId);
            default -> false;
        };
    }

    public boolean canViewTransfer(User actor, InterWarehouseTransfer transfer) {
        return canViewTransfer(actor, loadWarehouseIds(actor), transfer);
    }

    /**
     * Map transfer to response using the already eager-loaded items collection
     * (avoids redundant repository query when items were fetched via JOIN FETCH).
     */
    public InterWarehouseTransferResponse toResponseEager(InterWarehouseTransfer transfer) {
        TransferTripAlert alert = summarizeTripAlert(transfer);
        return transferMapper.toResponse(transfer, transfer.getItems(), alert.warningActive(), alert.overdue(), alert.message());
    }

    public InterWarehouseTransferResponse toResponse(InterWarehouseTransfer transfer) {
        TransferTripAlert alert = summarizeTripAlert(transfer);
        return transferMapper.toResponse(transfer, items(transfer), alert.warningActive(), alert.overdue(), alert.message());
    }

    public void audit(InterWarehouseTransfer transfer, User actor, AuditAction action,
                      Map<String, Object> before, Map<String, Object> after) {
        auditUtil.logChange(actor, action, ENTITY, transfer.getId(), transfer.getTransferNumber(), before, after);
    }

    public Map<String, Object> snapshot(InterWarehouseTransfer transfer) {
        return PartnerAuditUtil.values(
                "transferNumber", transfer.getTransferNumber(),
                "externalInstructionCode", transfer.getExternalInstructionCode(),
                "sourceWarehouseId", transfer.getSourceWarehouse() == null ? null : transfer.getSourceWarehouse().getId(),
                "destinationWarehouseId", transfer.getDestinationWarehouse() == null ? null : transfer.getDestinationWarehouse().getId(),
                "status", transfer.getStatus(),
                "tripId", transfer.getTrip() == null ? null : transfer.getTrip().getId(),
                "tripPlannedStartAt", transfer.getTrip() == null ? null : transfer.getTrip().getPlannedStartAt(),
                "tripPlannedEndAt", transfer.getTrip() == null ? null : transfer.getTrip().getPlannedEndAt(),
                "documentDate", transfer.getDocumentDate(),
                "plannedDate", transfer.getPlannedDate(),
                "discrepancyReason", transfer.getDiscrepancyReason(),
                "rejectionReason", transfer.getRejectionReason(),
                "notes", transfer.getNotes());
    }

    public void applyTripDeadlineRules(InterWarehouseTransfer transfer) {
        Trip trip = transfer.getTrip();
        if (trip == null || trip.getPlannedEndAt() == null || transfer.getStatus() != InterWarehouseTransferStatus.APPROVED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(trip.getPlannedEndAt())) {
            return;
        }
        Map<String, Object> before = snapshot(transfer);
        for (InterWarehouseTransferItem item : items(transfer)) {
            item.setSentQty(null);
            transferItemRepository.save(item);
        }
        releaseReservations(transfer);
        trip.setStatus(TripStatus.CANCELLED);
        trip.setUpdatedAt(OffsetDateTime.now());
        transfer.setStatus(InterWarehouseTransferStatus.CANCELLED);
        transfer.setRejectionReason("AUTO_CANCELLED_TRANSFER_OVERDUE");
        transfer.setUpdatedAt(OffsetDateTime.now());
        tripRepository.save(trip);
        InterWarehouseTransfer saved = transferRepository.save(transfer);
        User auditActor = trip.getDispatcher() != null ? trip.getDispatcher() : transfer.getCreatedBy();
        audit(saved, auditActor, AuditAction.TRANSFER_CANCEL, before, snapshot(saved));
    }

    public TransferTripAlert summarizeTripAlert(InterWarehouseTransfer transfer) {
        Trip trip = transfer.getTrip();
        if (trip == null || trip.getPlannedEndAt() == null || isTerminalTransferStatus(transfer.getStatus())) {
            return new TransferTripAlert(false, false, null);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(trip.getPlannedEndAt())) {
            return new TransferTripAlert(true, true, "Chuyến đã quá hạn hoàn thành.");
        }
        LocalDateTime warningStart = trip.getPlannedEndAt().minusDays(TRANSFER_WARNING_WINDOW_DAYS);
        if (!now.isBefore(warningStart)) {
            return new TransferTripAlert(true, false, "Chuyến đang ở 3 ngày cuối trước hạn giao.");
        }
        return new TransferTripAlert(false, false, null);
    }

    public boolean isTerminalTransferStatus(InterWarehouseTransferStatus status) {
        return status == InterWarehouseTransferStatus.COMPLETED
                || status == InterWarehouseTransferStatus.COMPLETED_WITH_DISCREPANCY
                || status == InterWarehouseTransferStatus.CANCELLED
                || status == InterWarehouseTransferStatus.REJECTED;
    }

    public String generateTransferNumber() {
        String prefix = "TRF-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (transferRepository.existsByTransferNumber(candidate));
        return candidate;
    }

    public String generateTripNumber() {
        String prefix = "TTR-" + OffsetDateTime.now().toLocalDate().toString().replace("-", "");
        int sequence = 1;
        String candidate;
        do {
            candidate = prefix + "-" + String.format("%04d", sequence++);
        } while (tripRepository.existsByTripNumber(candidate));
        return candidate;
    }

    public BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record TransferTripAlert(boolean warningActive, boolean overdue, String message) {
    }
}
