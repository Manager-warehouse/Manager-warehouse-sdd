package com.wms.service;

import com.wms.dto.request.CreateStockTakeRequest;
import com.wms.dto.request.StockTakeCountRequest;
import com.wms.dto.request.StockTakeCountItemRequest;
import com.wms.dto.request.StockTakeRejectRequest;
import com.wms.dto.response.StockTakeItemResponse;
import com.wms.dto.response.StockTakeResponse;
import com.wms.dto.response.StockTakeSummaryResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.*;
import com.wms.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockTakeService {

    private static final String ENTITY_TYPE = "STOCK_TAKE";
    private static final BigDecimal AUTO_THRESHOLD = new BigDecimal("5000000");
    private static final BigDecimal CEO_THRESHOLD = new BigDecimal("100000000");

    private final StockTakeRepository stockTakeRepository;
    private final StockTakeItemRepository stockTakeItemRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseLocationRepository locationRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserWarehouseAssignmentRepository assignmentRepository;
    private final AuditLogService auditLogService;

    public StockTakeService(StockTakeRepository stockTakeRepository,
            StockTakeItemRepository stockTakeItemRepository,
            InventoryRepository inventoryRepository,
            WarehouseLocationRepository locationRepository,
            AdjustmentRepository adjustmentRepository,
            AccountingPeriodRepository accountingPeriodRepository,
            DocumentSequenceRepository documentSequenceRepository,
            WarehouseRepository warehouseRepository,
            UserWarehouseAssignmentRepository assignmentRepository,
            AuditLogService auditLogService) {
        this.stockTakeRepository = stockTakeRepository;
        this.stockTakeItemRepository = stockTakeItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.documentSequenceRepository = documentSequenceRepository;
        this.warehouseRepository = warehouseRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditLogService = auditLogService;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StockTakeSummaryResponse> getStockTakes(Long warehouseId, StockTakeStatus status, User actor) {
        requireWarehouseAccess(actor, warehouseId);
        List<StockTake> list = (status != null)
                ? stockTakeRepository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status)
                : stockTakeRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
        return list.stream().map(StockTakeSummaryResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StockTakeSummaryResponse> getStockTakes(Long warehouseId, StockTakeStatus status, User actor, Pageable pageable) {
        requireWarehouseAccess(actor, warehouseId);
        Page<StockTake> page = (status != null)
                ? stockTakeRepository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status, pageable)
                : stockTakeRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId, pageable);
        return page.map(StockTakeSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public StockTakeResponse getStockTakeById(Long id, User actor) {
        StockTake st = loadStockTake(id);
        requireWarehouseAccess(actor, st.getWarehouse().getId());
        List<StockTakeItemResponse> items = stockTakeItemRepository
                .findByStockTakeIdWithDetails(id)
                .stream().map(StockTakeItemResponse::from).collect(Collectors.toList());
        return StockTakeResponse.from(st, items);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse createStockTake(CreateStockTakeRequest req, User actor) {
        requireStockTakeRole(actor);
        requireWarehouseAccess(actor, req.getWarehouseId());

        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + req.getWarehouseId()));

        AccountingPeriod period = accountingPeriodRepository.findById(req.getAccountingPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AccountingPeriod not found: " + req.getAccountingPeriodId()));
        assertPeriodOpen(period);

        String number = generateStockTakeNumber();

        StockTake st = StockTake.builder()
                .stockTakeNumber(number)
                .warehouse(warehouse)
                .conductedBy(actor)
                .status(StockTakeStatus.DRAFT)
                .isEmployeeFault(false)
                .totalVarianceValue(BigDecimal.ZERO)
                .stockTakeDate(req.getStockTakeDate())
                .documentDate(req.getDocumentDate())
                .accountingPeriod(period)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        st = stockTakeRepository.save(st);

        // Populate items from current non-quarantine inventory
        List<Inventory> inventories = inventoryRepository.findActiveNonQuarantineByWarehouseId(req.getWarehouseId());
        List<StockTakeItem> items = new ArrayList<>();
        for (Inventory inv : inventories) {
            StockTakeItem item = StockTakeItem.builder()
                    .stockTake(st)
                    .product(inv.getProduct())
                    .batch(inv.getBatch())
                    .location(inv.getLocation())
                    .systemQty(inv.getTotalQty())
                    .actualQty(null) // not yet counted
                    .varianceQty(BigDecimal.ZERO)
                    .varianceValue(BigDecimal.ZERO)
                    .build();
            items.add(item);
        }
        stockTakeItemRepository.saveAll(items);

        auditLogService.log(actor, AuditAction.STOCKTAKE_CREATE, ENTITY_TYPE,
                st.getId(), number, warehouse.getId(), null, snapshotHeader(st));

        List<StockTakeItemResponse> itemResponses = items.stream()
                .map(StockTakeItemResponse::from).collect(Collectors.toList());
        return StockTakeResponse.from(st, itemResponses);
    }

    // ─── Start ────────────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse startStockTake(Long id, User actor) {
        requireStockTakeRole(actor);
        StockTake st = loadStockTake(id);
        requireWarehouseAccess(actor, st.getWarehouse().getId());

        if (st.getStatus() != StockTakeStatus.DRAFT) {
            throw new StockTakeException("INVALID_STATE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "StockTake can only be started from DRAFT status, current: " + st.getStatus());
        }

        st.setStatus(StockTakeStatus.IN_PROGRESS);
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        // Lock all locations referenced in items
        lockLocations(st);

        auditLogService.log(actor, AuditAction.STOCKTAKE_START, ENTITY_TYPE,
                st.getId(), st.getStockTakeNumber(), st.getWarehouse().getId(), null, snapshotHeader(st));

        return buildResponse(st);
    }

    // ─── Record Count ─────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse recordCount(Long id, StockTakeCountRequest req, User actor) {
        requireStockTakeRole(actor);
        StockTake st = loadStockTake(id);
        requireWarehouseAccess(actor, st.getWarehouse().getId());

        // After a REJECTED stocktake, the storekeeper may edit counts and re-submit
        // (Spec 006,
        // feature-manager EARS + Scenario 6), so REJECTED is also a valid editing
        // state.
        if (st.getStatus() != StockTakeStatus.IN_PROGRESS && st.getStatus() != StockTakeStatus.REJECTED) {
            throw new StockTakeException("INVALID_STATE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Count can only be recorded while IN_PROGRESS or REJECTED, current: " + st.getStatus());
        }

        boolean anyEmployeeFault = false;
        for (StockTakeCountItemRequest countReq : req.getItems()) {
            if (countReq.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new StockTakeException("INVALID_COUNT_QTY", HttpStatus.BAD_REQUEST,
                        "actual_qty must be >= 0 for item " + countReq.getItemId());
            }
            if (Boolean.TRUE.equals(countReq.getIsEmployeeFault())
                    && (countReq.getNotes() == null || countReq.getNotes().isBlank())) {
                throw new StockTakeException("EMPLOYEE_FAULT_REASON_REQUIRED", HttpStatus.BAD_REQUEST,
                        "notes is required when is_employee_fault=true for item " + countReq.getItemId());
            }

            StockTakeItem item = stockTakeItemRepository.findById(countReq.getItemId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("StockTakeItem not found: " + countReq.getItemId()));

            if (!item.getStockTake().getId().equals(id)) {
                throw new StockTakeException("INVALID_ARGUMENT", HttpStatus.BAD_REQUEST,
                        "Item " + countReq.getItemId() + " does not belong to stocktake " + id);
            }

            // Notes (variance reason) is mandatory when actual_qty differs from system_qty
            if (countReq.getActualQty().compareTo(item.getSystemQty()) != 0
                    && (countReq.getNotes() == null || countReq.getNotes().isBlank())) {
                throw new StockTakeException("VARIANCE_REASON_REQUIRED", HttpStatus.BAD_REQUEST,
                        "Lý do chênh lệch (notes) bắt buộc khi số lượng thực tế khác hệ thống — item "
                                + countReq.getItemId());
            }

            // Load inventory to get cost price
            Inventory inv = inventoryRepository.findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
                    st.getWarehouse().getId(), item.getProduct().getId(),
                    item.getBatch().getId(), item.getLocation().getId())
                    .orElse(null);
            BigDecimal costPrice = (inv != null) ? inv.getCostPrice() : BigDecimal.ZERO;

            BigDecimal varianceQty = countReq.getActualQty().subtract(item.getSystemQty());
            BigDecimal varianceValue = varianceQty.multiply(costPrice);

            item.setActualQty(countReq.getActualQty());
            item.setVarianceQty(varianceQty);
            item.setVarianceValue(varianceValue);
            item.setNotes(countReq.getNotes());
            stockTakeItemRepository.save(item);

            if (Boolean.TRUE.equals(countReq.getIsEmployeeFault())) {
                anyEmployeeFault = true;
            }
        }

        if (anyEmployeeFault) {
            st.setIsEmployeeFault(true);
        }
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        auditLogService.log(actor, AuditAction.STOCKTAKE_COUNT_UPDATE, ENTITY_TYPE,
                st.getId(), st.getStockTakeNumber(), st.getWarehouse().getId(), null, snapshotHeader(st));

        return buildResponse(st);
    }

    // ─── Complete ─────────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse completeStockTake(Long id, User actor) {
        requireStockTakeRole(actor);
        StockTake st = loadStockTake(id);
        requireWarehouseAccess(actor, st.getWarehouse().getId());

        // A REJECTED stocktake can be re-submitted after the storekeeper edits counts
        // (Spec 006, feature-manager EARS + Scenario 6).
        if (st.getStatus() != StockTakeStatus.IN_PROGRESS && st.getStatus() != StockTakeStatus.REJECTED) {
            throw new StockTakeException("INVALID_STATE",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Complete requires IN_PROGRESS or REJECTED status, current: " + st.getStatus());
        }

        if (stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(id)) {
            throw new StockTakeException("INCOMPLETE_COUNT", HttpStatus.BAD_REQUEST,
                    "All items must have actual_qty recorded before completing");
        }

        // Recalculate total variance
        List<StockTakeItem> items = stockTakeItemRepository.findByStockTakeId(id);
        BigDecimal totalVariance = items.stream()
                .map(StockTakeItem::getVarianceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        st.setTotalVarianceValue(totalVariance);

        // Determine approval level
        BigDecimal absVariance = totalVariance.abs();
        ApprovalLevel level;
        if (absVariance.compareTo(AUTO_THRESHOLD) < 0) {
            level = ApprovalLevel.AUTO;
        } else if (absVariance.compareTo(CEO_THRESHOLD) <= 0 && !Boolean.TRUE.equals(st.getIsEmployeeFault())) {
            level = ApprovalLevel.MANAGER;
        } else {
            level = ApprovalLevel.CEO;
        }
        st.setApprovalLevel(level);
        st.setStatus(StockTakeStatus.PENDING_APPROVAL);
        st.setRejectionReason(null); // clear any prior rejection reason on re-submit
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        auditLogService.log(actor, AuditAction.STOCKTAKE_COMPLETE, ENTITY_TYPE,
                st.getId(), st.getStockTakeNumber(), st.getWarehouse().getId(), null, snapshotHeader(st));

        if (level == ApprovalLevel.AUTO) {
            executeApproval(st, null);
        }

        return buildResponse(st);
    }

    // ─── Approve (Manager) ────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse approveStockTake(Long id, User actor) {
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.CEO) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "Only WAREHOUSE_MANAGER or CEO can approve stocktakes");
        }
        StockTake st = stockTakeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake not found: " + id));
        requireWarehouseAccess(actor, st.getWarehouse().getId());

        assertPendingApproval(st);
        if (st.getApprovalLevel() == ApprovalLevel.CEO && actor.getRole() != UserRole.CEO
                && actor.getRole() != UserRole.ADMIN) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "This stocktake requires CEO approval");
        }
        assertPeriodOpen(st.getAccountingPeriod());
        executeApproval(st, actor);
        return buildResponse(st);
    }

    // ─── Approve (CEO) ────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse approveCeoStockTake(Long id, User actor) {
        if (actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "Only CEO can call the CEO approve endpoint");
        }
        StockTake st = stockTakeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake not found: " + id));
        requireWarehouseAccess(actor, st.getWarehouse().getId());
        assertPendingApproval(st);
        assertPeriodOpen(st.getAccountingPeriod());
        executeApproval(st, actor);
        return buildResponse(st);
    }

    // ─── Reject (Manager) ─────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse rejectStockTake(Long id, StockTakeRejectRequest req, User actor) {
        if (actor.getRole() != UserRole.WAREHOUSE_MANAGER && actor.getRole() != UserRole.ADMIN) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "Only WAREHOUSE_MANAGER can reject at MANAGER level");
        }
        StockTake st = stockTakeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake not found: " + id));
        requireWarehouseAccess(actor, st.getWarehouse().getId());
        assertPendingApproval(st);
        if (st.getApprovalLevel() == ApprovalLevel.CEO) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "This stocktake requires CEO approval/rejection");
        }
        return doReject(st, req.getRejectionReason(), actor);
    }

    // ─── Reject (CEO) ─────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse rejectCeoStockTake(Long id, StockTakeRejectRequest req, User actor) {
        if (actor.getRole() != UserRole.CEO && actor.getRole() != UserRole.ADMIN) {
            throw new StockTakeException("APPROVAL_LEVEL_MISMATCH", HttpStatus.FORBIDDEN,
                    "Only CEO can call the CEO reject endpoint");
        }
        StockTake st = stockTakeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake not found: " + id));
        requireWarehouseAccess(actor, st.getWarehouse().getId());
        assertPendingApproval(st);
        return doReject(st, req.getRejectionReason(), actor);
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    @Transactional
    public StockTakeResponse cancelStockTake(Long id, User actor) {
        requireStockTakeRole(actor);
        StockTake st = loadStockTake(id);
        requireWarehouseAccess(actor, st.getWarehouse().getId());

        if (st.getStatus() != StockTakeStatus.DRAFT && st.getStatus() != StockTakeStatus.IN_PROGRESS) {
            throw new StockTakeException("STOCK_TAKE_NOT_CANCELLABLE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "StockTake can only be cancelled when DRAFT or IN_PROGRESS, current: " + st.getStatus());
        }

        boolean wasInProgress = st.getStatus() == StockTakeStatus.IN_PROGRESS;
        st.setStatus(StockTakeStatus.CANCELLED);
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        if (wasInProgress) {
            unlockLocations(st.getId());
        }

        auditLogService.log(actor, AuditAction.STOCKTAKE_CANCEL, ENTITY_TYPE,
                st.getId(), st.getStockTakeNumber(), st.getWarehouse().getId(), null, snapshotHeader(st));

        return buildResponse(st);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void executeApproval(StockTake st, User approver) {
        List<StockTakeItem> items = stockTakeItemRepository.findByStockTakeIdWithDetails(st.getId());
        for (StockTakeItem item : items) {
            if (item.getVarianceQty() == null || item.getVarianceQty().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Inventory inv = inventoryRepository.findByWarehouseProductBatchLocationForUpdate(
                    st.getWarehouse().getId(),
                    item.getProduct().getId(),
                    item.getBatch().getId(),
                    item.getLocation().getId())
                    .orElse(null);

            if (inv != null) {
                BigDecimal newQty = item.getActualQty();
                if (newQty.subtract(inv.getReservedQty()).compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRuleViolationException(
                            "INVENTORY_INVARIANT_VIOLATED: Cannot set qty below reserved for product "
                                    + item.getProduct().getSku());
                }
                inv.setTotalQty(newQty);
                inv.setUpdatedAt(OffsetDateTime.now());
                inventoryRepository.save(inv);
            }

            // Create adjustment record
            String adjNumber = "ADJ-ST-" + st.getId() + "-" + item.getId();
            Adjustment adj = Adjustment.builder()
                    .adjustmentNumber(adjNumber)
                    .warehouse(st.getWarehouse())
                    .product(item.getProduct())
                    .batch(item.getBatch())
                    .location(item.getLocation())
                    .quantityAdjustment(item.getVarianceQty())
                    .type(AdjustmentType.STOCK_TAKE)
                    .referenceId(st.getId())
                    .referenceType("STOCK_TAKE")
                    .reason("StockTake adjustment: " + st.getStockTakeNumber())
                    .approvedBy(approver)
                    .approvedAt(OffsetDateTime.now())
                    .documentDate(st.getDocumentDate())
                    .accountingPeriod(st.getAccountingPeriod())
                    .createdBy(st.getConductedBy())
                    .createdAt(OffsetDateTime.now())
                    .build();
            adjustmentRepository.save(adj);
        }

        st.setStatus(StockTakeStatus.APPROVED);
        st.setApprovedBy(approver);
        st.setApprovedAt(OffsetDateTime.now());
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        unlockLocations(st.getId());

        AuditAction action = (approver == null) ? AuditAction.STOCKTAKE_AUTO_APPROVE : AuditAction.STOCKTAKE_APPROVE;
        auditLogService.log(approver != null ? approver : st.getConductedBy(),
                action, ENTITY_TYPE, st.getId(), st.getStockTakeNumber(),
                st.getWarehouse().getId(), null, snapshotHeader(st));
    }

    private StockTakeResponse doReject(StockTake st, String reason, User actor) {
        st.setStatus(StockTakeStatus.REJECTED);
        st.setRejectionReason(reason);
        st.setUpdatedAt(OffsetDateTime.now());
        stockTakeRepository.save(st);

        unlockLocations(st.getId());

        auditLogService.log(actor, AuditAction.STOCKTAKE_REJECT, ENTITY_TYPE,
                st.getId(), st.getStockTakeNumber(), st.getWarehouse().getId(), null, snapshotHeader(st));

        return buildResponse(st);
    }

    private void lockLocations(StockTake st) {
        List<StockTakeItem> items = stockTakeItemRepository.findByStockTakeId(st.getId());
        List<Long> locationIds = items.stream()
                .map(i -> i.getLocation().getId()).distinct().collect(Collectors.toList());
        List<WarehouseLocation> locations = locationRepository.findByIdIn(locationIds);
        for (WarehouseLocation loc : locations) {
            loc.setIsLocked(true);
            loc.setLockedByStockTakeId(st.getId());
        }
        locationRepository.saveAll(locations);
    }

    private void unlockLocations(Long stockTakeId) {
        List<WarehouseLocation> locations = locationRepository.findByLockedByStockTakeId(stockTakeId);
        for (WarehouseLocation loc : locations) {
            loc.setIsLocked(false);
            loc.setLockedByStockTakeId(null);
        }
        locationRepository.saveAll(locations);
    }

    private String generateStockTakeNumber() {
        DocumentSequence seq = documentSequenceRepository.findBySequenceKeyForUpdate("ST")
                .orElseThrow(() -> new ResourceNotFoundException("DocumentSequence not found for key: ST"));
        long next = seq.getNextValue();
        seq.setNextValue(next + 1);
        seq.setUpdatedAt(OffsetDateTime.now());
        documentSequenceRepository.save(seq);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("ST-%s-%06d", date, next);
    }

    private void assertPeriodOpen(AccountingPeriod period) {
        if (period == null || period.getStatus() != AccountingPeriodStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "ACCOUNTING_PERIOD_CLOSED: The accounting period is not OPEN");
        }
    }

    private void assertPendingApproval(StockTake st) {
        if (st.getStatus() == StockTakeStatus.APPROVED) {
            throw new StockTakeException("STOCK_TAKE_ALREADY_APPROVED", HttpStatus.CONFLICT,
                    "StockTake is already APPROVED");
        }
        if (st.getStatus() != StockTakeStatus.PENDING_APPROVAL) {
            throw new StockTakeException("INVALID_STATE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "StockTake must be PENDING_APPROVAL for approval/rejection, current: " + st.getStatus());
        }
    }

    private void requireStockTakeRole(User actor) {
        UserRole role = actor.getRole();
        if (role != UserRole.WAREHOUSE_MANAGER && role != UserRole.STOREKEEPER
                && role != UserRole.ADMIN && role != UserRole.CEO) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Role " + role + " is not authorized for stocktake operations");
        }
    }

    private void requireWarehouseAccess(User actor, Long warehouseId) {
        if (actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.CEO) {
            return;
        }
        boolean assigned = assignmentRepository.findWarehouseIdsByUserId(actor.getId())
                .contains(warehouseId);
        if (!assigned) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "User is not assigned to warehouse: " + warehouseId);
        }
    }

    private StockTake loadStockTake(Long id) {
        return stockTakeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("StockTake not found: " + id));
    }

    private StockTakeResponse buildResponse(StockTake st) {
        List<StockTakeItemResponse> items = stockTakeItemRepository
                .findByStockTakeIdWithDetails(st.getId())
                .stream().map(StockTakeItemResponse::from).collect(Collectors.toList());
        return StockTakeResponse.from(st, items);
    }

    private Map<String, Object> snapshotHeader(StockTake st) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stock_take_number", st.getStockTakeNumber());
        m.put("status", st.getStatus());
        m.put("approval_level", st.getApprovalLevel());
        m.put("is_employee_fault", st.getIsEmployeeFault());
        m.put("total_variance_value", st.getTotalVarianceValue());
        return m;
    }
}
