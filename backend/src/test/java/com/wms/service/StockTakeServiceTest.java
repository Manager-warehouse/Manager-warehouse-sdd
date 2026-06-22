package com.wms.service;

import com.wms.dto.request.CreateStockTakeRequest;
import com.wms.dto.request.StockTakeCountItemRequest;
import com.wms.dto.request.StockTakeCountRequest;
import com.wms.dto.request.StockTakeRejectRequest;
import com.wms.dto.response.StockTakeResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.StockTakeException;
import com.wms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockTakeService — Spec 006 (US-WMS-13).
 *
 * <p>Covers: create (happy + period closed), start (happy + invalid state),
 * count validation (negative qty, employee-fault reason), complete approval-routing
 * (AUTO / MANAGER / CEO / employee-fault escalation), approve (happy, level mismatch,
 * already approved, inventory invariant), reject, and cancel.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockTakeServiceTest {

    @Mock private StockTakeRepository stockTakeRepository;
    @Mock private StockTakeItemRepository stockTakeItemRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseLocationRepository locationRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DocumentSequenceRepository documentSequenceRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private UserWarehouseAssignmentRepository assignmentRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private StockTakeService service;

    private Warehouse warehouse;
    private User storekeeper;
    private User manager;
    private User ceo;
    private AccountingPeriod openPeriod;
    private Product product;
    private Batch batch;
    private WarehouseLocation location;

    private static final Long WH_ID = 10L;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(WH_ID);
        warehouse.setCode("WH-HP");

        storekeeper = userWith(3L, UserRole.STOREKEEPER);
        manager = userWith(5L, UserRole.WAREHOUSE_MANAGER);
        ceo = userWith(1L, UserRole.CEO);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(1L);
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);

        product = new Product();
        product.setId(100L);
        product.setSku("SKU-001");

        batch = new Batch();
        batch.setId(200L);

        location = new WarehouseLocation();
        location.setId(300L);
        location.setCode("WH-HP.A.01");
        location.setIsLocked(false);

        // Default: storekeeper & manager are assigned to the warehouse
        when(assignmentRepository.findWarehouseIdsByUserId(anyLong())).thenReturn(List.of(WH_ID));
    }

    private User userWith(Long id, UserRole role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    private StockTake stockTake(StockTakeStatus status) {
        return StockTake.builder()
                .id(1L)
                .stockTakeNumber("ST-20260617-000001")
                .warehouse(warehouse)
                .conductedBy(storekeeper)
                .status(status)
                .isEmployeeFault(false)
                .totalVarianceValue(BigDecimal.ZERO)
                .stockTakeDate(LocalDate.of(2026, 6, 17))
                .documentDate(LocalDate.of(2026, 6, 17))
                .accountingPeriod(openPeriod)
                .build();
    }

    private StockTakeItem item(BigDecimal systemQty, BigDecimal actualQty, BigDecimal varianceQty, BigDecimal varianceValue) {
        return StockTakeItem.builder()
                .id(50L)
                .stockTake(stockTake(StockTakeStatus.IN_PROGRESS))
                .product(product)
                .batch(batch)
                .location(location)
                .systemQty(systemQty)
                .actualQty(actualQty)
                .varianceQty(varianceQty)
                .varianceValue(varianceValue)
                .build();
    }

    // ─── Create ─────────────────────────────────────────────────────────────────

    @Test
    void createStockTake_validRequest_createsDraftWithItemsAndAudit() {
        CreateStockTakeRequest req = new CreateStockTakeRequest();
        req.setWarehouseId(WH_ID);
        req.setStockTakeDate(LocalDate.of(2026, 6, 17));
        req.setDocumentDate(LocalDate.of(2026, 6, 17));
        req.setAccountingPeriodId(1L);

        when(warehouseRepository.findById(WH_ID)).thenReturn(Optional.of(warehouse));
        when(accountingPeriodRepository.findById(1L)).thenReturn(Optional.of(openPeriod));
        DocumentSequence seq = new DocumentSequence();
        seq.setSequenceKey("ST");
        seq.setNextValue(1L);
        when(documentSequenceRepository.findBySequenceKeyForUpdate("ST")).thenReturn(Optional.of(seq));
        when(stockTakeRepository.save(any(StockTake.class))).thenAnswer(i -> i.getArgument(0));

        Inventory inv = new Inventory();
        inv.setProduct(product);
        inv.setBatch(batch);
        inv.setLocation(location);
        inv.setTotalQty(new BigDecimal("100"));
        when(inventoryRepository.findActiveNonQuarantineByWarehouseId(WH_ID)).thenReturn(List.of(inv));

        StockTakeResponse res = service.createStockTake(req, storekeeper);

        assertEquals(StockTakeStatus.DRAFT, res.getStatus());
        verify(stockTakeItemRepository).saveAll(argThat((List<StockTakeItem> items) ->
                items.size() == 1 && items.get(0).getSystemQty().compareTo(new BigDecimal("100")) == 0
                        && items.get(0).getActualQty() == null));
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.STOCKTAKE_CREATE), eq("STOCK_TAKE"),
                any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void createStockTake_periodClosed_throwsBusinessRuleViolation() {
        AccountingPeriod closed = new AccountingPeriod();
        closed.setId(2L);
        closed.setStatus(AccountingPeriodStatus.CLOSED);

        CreateStockTakeRequest req = new CreateStockTakeRequest();
        req.setWarehouseId(WH_ID);
        req.setStockTakeDate(LocalDate.now());
        req.setDocumentDate(LocalDate.now());
        req.setAccountingPeriodId(2L);

        when(warehouseRepository.findById(WH_ID)).thenReturn(Optional.of(warehouse));
        when(accountingPeriodRepository.findById(2L)).thenReturn(Optional.of(closed));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.createStockTake(req, storekeeper));
        assertTrue(ex.getMessage().contains("ACCOUNTING_PERIOD_CLOSED"));
    }

    // ─── Start ──────────────────────────────────────────────────────────────────

    @Test
    void startStockTake_fromDraft_transitionsToInProgressAndLocksLocations() {
        StockTake st = stockTake(StockTakeStatus.DRAFT);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(item(
                new BigDecimal("100"), null, BigDecimal.ZERO, BigDecimal.ZERO)));
        when(locationRepository.findByIdIn(anyList())).thenReturn(List.of(location));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of());

        StockTakeResponse res = service.startStockTake(1L, storekeeper);

        assertEquals(StockTakeStatus.IN_PROGRESS, res.getStatus());
        assertTrue(location.getIsLocked());
        assertEquals(1L, location.getLockedByStockTakeId());
        verify(locationRepository).saveAll(anyList());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.STOCKTAKE_START), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void startStockTake_notDraft_throwsStockTakeException() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.startStockTake(1L, storekeeper));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        assertEquals("INVALID_STATE", ex.getCode());
    }

    // ─── Record Count ─────────────────────────────────────────────────────────

    @Test
    void recordCount_negativeActualQty_throwsInvalidCountQty() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeCountItemRequest ci = new StockTakeCountItemRequest();
        ci.setItemId(50L);
        ci.setActualQty(new BigDecimal("-1"));
        StockTakeCountRequest req = new StockTakeCountRequest();
        req.setItems(List.of(ci));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.recordCount(1L, req, storekeeper));
        assertEquals("INVALID_COUNT_QTY", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void recordCount_employeeFaultWithoutNotes_throwsReasonRequired() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeCountItemRequest ci = new StockTakeCountItemRequest();
        ci.setItemId(50L);
        ci.setActualQty(new BigDecimal("90"));
        ci.setIsEmployeeFault(true);
        ci.setNotes("   ");
        StockTakeCountRequest req = new StockTakeCountRequest();
        req.setItems(List.of(ci));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.recordCount(1L, req, storekeeper));
        assertEquals("EMPLOYEE_FAULT_REASON_REQUIRED", ex.getCode());
    }

    @Test
    void recordCount_validCount_computesVarianceAndSavesItem() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeItem item = item(new BigDecimal("100"), null, BigDecimal.ZERO, BigDecimal.ZERO);
        when(stockTakeItemRepository.findById(50L)).thenReturn(Optional.of(item));

        Inventory inv = new Inventory();
        inv.setCostPrice(new BigDecimal("50000"));
        when(inventoryRepository.findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
                WH_ID, 100L, 200L, 300L)).thenReturn(Optional.of(inv));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(item));

        StockTakeCountItemRequest ci = new StockTakeCountItemRequest();
        ci.setItemId(50L);
        ci.setActualQty(new BigDecimal("88"));
        ci.setNotes("Hư hỏng trong lưu kho");
        StockTakeCountRequest req = new StockTakeCountRequest();
        req.setItems(List.of(ci));

        service.recordCount(1L, req, storekeeper);

        assertEquals(new BigDecimal("88"), item.getActualQty());
        assertEquals(0, item.getVarianceQty().compareTo(new BigDecimal("-12")));
        assertEquals(0, item.getVarianceValue().compareTo(new BigDecimal("-600000")));
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.STOCKTAKE_COUNT_UPDATE), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void recordCount_afterRejected_isAllowed() {
        StockTake st = stockTake(StockTakeStatus.REJECTED);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeItem item = item(new BigDecimal("100"), null, BigDecimal.ZERO, BigDecimal.ZERO);
        when(stockTakeItemRepository.findById(50L)).thenReturn(Optional.of(item));
        Inventory inv = new Inventory();
        inv.setCostPrice(new BigDecimal("50000"));
        when(inventoryRepository.findByWarehouseIdAndProductIdAndBatchIdAndLocationId(
                WH_ID, 100L, 200L, 300L)).thenReturn(Optional.of(inv));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(item));

        StockTakeCountItemRequest ci = new StockTakeCountItemRequest();
        ci.setItemId(50L);
        ci.setActualQty(new BigDecimal("95"));
        ci.setNotes("Đếm lại sau khi bị từ chối");
        StockTakeCountRequest req = new StockTakeCountRequest();
        req.setItems(List.of(ci));

        // Should not throw — REJECTED is a valid editing state for re-submission.
        service.recordCount(1L, req, storekeeper);
        assertEquals(new BigDecimal("95"), item.getActualQty());
    }

    @Test
    void completeStockTake_afterRejected_reroutesToPendingAndClearsReason() {
        StockTake st = stockTake(StockTakeStatus.REJECTED);
        st.setRejectionReason("Recount needed");
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(false);
        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("0"),
                new BigDecimal("-100"), new BigDecimal("-50000000"));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(it));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        StockTakeResponse res = service.completeStockTake(1L, storekeeper);

        assertEquals(StockTakeStatus.PENDING_APPROVAL, res.getStatus());
        assertEquals(ApprovalLevel.MANAGER, st.getApprovalLevel());
        assertNull(st.getRejectionReason());
    }

    // ─── Complete (approval routing) ────────────────────────────────────────────

    @Test
    void completeStockTake_incompleteCount_throwsIncompleteCount() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(true);

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.completeStockTake(1L, storekeeper));
        assertEquals("INCOMPLETE_COUNT", ex.getCode());
    }

    @Test
    void completeStockTake_smallVariance_autoApproves() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(false);
        // total variance 1,000,000 < 5,000,000 → AUTO
        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("98"),
                new BigDecimal("-2"), new BigDecimal("-1000000"));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(it));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(WH_ID, 100L, 200L, 300L))
                .thenReturn(Optional.empty());
        when(locationRepository.findByLockedByStockTakeId(1L)).thenReturn(List.of());

        StockTakeResponse res = service.completeStockTake(1L, storekeeper);

        assertEquals(ApprovalLevel.AUTO, st.getApprovalLevel());
        assertEquals(StockTakeStatus.APPROVED, res.getStatus());
        verify(auditLogService).log(any(), eq(AuditAction.STOCKTAKE_AUTO_APPROVE), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void completeStockTake_midVarianceNoFault_routesToManager() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(false);
        // -50,000,000 → between 5M and 100M, not employee fault → MANAGER
        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("0"),
                new BigDecimal("-100"), new BigDecimal("-50000000"));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(it));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        StockTakeResponse res = service.completeStockTake(1L, storekeeper);

        assertEquals(ApprovalLevel.MANAGER, st.getApprovalLevel());
        assertEquals(StockTakeStatus.PENDING_APPROVAL, res.getStatus());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.STOCKTAKE_COMPLETE), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void completeStockTake_largeVariance_routesToCeo() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(false);
        // -150,000,000 > 100M → CEO
        StockTakeItem it = item(new BigDecimal("300"), new BigDecimal("0"),
                new BigDecimal("-300"), new BigDecimal("-150000000"));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(it));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        service.completeStockTake(1L, storekeeper);

        assertEquals(ApprovalLevel.CEO, st.getApprovalLevel());
        assertEquals(StockTakeStatus.PENDING_APPROVAL, st.getStatus());
    }

    @Test
    void completeStockTake_employeeFaultEscalatesToCeo() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        st.setIsEmployeeFault(true);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(stockTakeItemRepository.existsByStockTakeIdAndActualQtyIsNull(1L)).thenReturn(false);
        // mid value but employee fault → CEO
        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("80"),
                new BigDecimal("-20"), new BigDecimal("-10000000"));
        when(stockTakeItemRepository.findByStockTakeId(1L)).thenReturn(List.of(it));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        service.completeStockTake(1L, storekeeper);

        assertEquals(ApprovalLevel.CEO, st.getApprovalLevel());
    }

    // ─── Approve ────────────────────────────────────────────────────────────────

    @Test
    void approveStockTake_managerOnCeoLevel_throwsApprovalLevelMismatch() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        st.setApprovalLevel(ApprovalLevel.CEO);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.approveStockTake(1L, manager));
        assertEquals("APPROVAL_LEVEL_MISMATCH", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void approveStockTake_managerLevelHappy_setsApprovedUpdatesInventoryCreatesAdjustment() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        st.setApprovalLevel(ApprovalLevel.MANAGER);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));

        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("90"),
                new BigDecimal("-10"), new BigDecimal("-5000000"));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        Inventory inv = new Inventory();
        inv.setTotalQty(new BigDecimal("100"));
        inv.setReservedQty(BigDecimal.ZERO);
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(WH_ID, 100L, 200L, 300L))
                .thenReturn(Optional.of(inv));
        when(locationRepository.findByLockedByStockTakeId(1L)).thenReturn(List.of());

        StockTakeResponse res = service.approveStockTake(1L, manager);

        assertEquals(StockTakeStatus.APPROVED, res.getStatus());
        assertEquals(0, inv.getTotalQty().compareTo(new BigDecimal("90"))); // set to actual
        verify(adjustmentRepository).save(any(Adjustment.class));
        verify(auditLogService).log(eq(manager), eq(AuditAction.STOCKTAKE_APPROVE), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void approveStockTake_alreadyApproved_throwsConflict() {
        StockTake st = stockTake(StockTakeStatus.APPROVED);
        st.setApprovalLevel(ApprovalLevel.MANAGER);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.approveStockTake(1L, manager));
        assertEquals("STOCK_TAKE_ALREADY_APPROVED", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void approveStockTake_actualQtyBelowReserved_throwsInventoryInvariant() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        st.setApprovalLevel(ApprovalLevel.MANAGER);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));

        StockTakeItem it = item(new BigDecimal("100"), new BigDecimal("5"),
                new BigDecimal("-95"), new BigDecimal("-47500000"));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of(it));

        Inventory inv = new Inventory();
        inv.setTotalQty(new BigDecimal("100"));
        inv.setReservedQty(new BigDecimal("10")); // actual(5) - reserved(10) < 0
        when(inventoryRepository.findByWarehouseProductBatchLocationForUpdate(WH_ID, 100L, 200L, 300L))
                .thenReturn(Optional.of(inv));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.approveStockTake(1L, manager));
        assertTrue(ex.getMessage().contains("INVENTORY_INVARIANT_VIOLATED"));
    }

    @Test
    void approveCeoStockTake_byManager_throwsApprovalLevelMismatch() {
        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.approveCeoStockTake(1L, manager));
        assertEquals("APPROVAL_LEVEL_MISMATCH", ex.getCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ─── Reject ─────────────────────────────────────────────────────────────────

    @Test
    void rejectStockTake_managerLevel_setsRejectedWithReasonAndUnlocks() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        st.setApprovalLevel(ApprovalLevel.MANAGER);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));
        when(locationRepository.findByLockedByStockTakeId(1L)).thenReturn(List.of(location));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of());

        StockTakeRejectRequest req = new StockTakeRejectRequest();
        req.setRejectionReason("Count looks wrong, recount needed");

        StockTakeResponse res = service.rejectStockTake(1L, req, manager);

        assertEquals(StockTakeStatus.REJECTED, res.getStatus());
        assertEquals("Count looks wrong, recount needed", st.getRejectionReason());
        verify(locationRepository).saveAll(anyList());
        verify(auditLogService).log(eq(manager), eq(AuditAction.STOCKTAKE_REJECT), any(), any(), any(), eq(WH_ID), any(), any());
    }

    @Test
    void rejectStockTake_ceoLevelByManager_throwsApprovalLevelMismatch() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        st.setApprovalLevel(ApprovalLevel.CEO);
        when(stockTakeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(st));

        StockTakeRejectRequest req = new StockTakeRejectRequest();
        req.setRejectionReason("nope");

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.rejectStockTake(1L, req, manager));
        assertEquals("APPROVAL_LEVEL_MISMATCH", ex.getCode());
    }

    // ─── Cancel ─────────────────────────────────────────────────────────────────

    @Test
    void cancelStockTake_pendingApproval_throwsNotCancellable() {
        StockTake st = stockTake(StockTakeStatus.PENDING_APPROVAL);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));

        StockTakeException ex = assertThrows(StockTakeException.class,
                () -> service.cancelStockTake(1L, storekeeper));
        assertEquals("STOCK_TAKE_NOT_CANCELLABLE", ex.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void cancelStockTake_inProgress_cancelsAndUnlocks() {
        StockTake st = stockTake(StockTakeStatus.IN_PROGRESS);
        when(stockTakeRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(st));
        when(locationRepository.findByLockedByStockTakeId(1L)).thenReturn(List.of(location));
        when(stockTakeItemRepository.findByStockTakeIdWithDetails(1L)).thenReturn(List.of());

        StockTakeResponse res = service.cancelStockTake(1L, storekeeper);

        assertEquals(StockTakeStatus.CANCELLED, res.getStatus());
        verify(locationRepository).saveAll(anyList());
        verify(auditLogService).log(eq(storekeeper), eq(AuditAction.STOCKTAKE_CANCEL), any(), any(), any(), eq(WH_ID), any(), any());
    }
}
