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

import com.wms.dto.request.PriceHistoryCreateRequest;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.PriceImportResponse;
import com.wms.dto.response.ProductPriceHistoryResponse;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.access_control.User;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.enums.access_control.UserRole;
import com.wms.exception.PriceHistoryException;
import com.wms.repository.NotificationRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.product_catalog.ProductRepository;
import com.wms.repository.UserRepository;
import com.wms.repository.UserWarehouseAssignmentRepository;
import com.wms.repository.WarehouseRepository;
import org.springframework.security.access.AccessDeniedException;
import com.wms.service.price_management.impl.PriceHistoryServiceImpl;
import com.wms.util.PartnerAuditUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock PriceHistoryRepository priceHistoryRepository;
    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock PartnerAuditUtil auditUtil;
    @Mock AccountingPeriodService accountingPeriodService;
    @Mock UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    PriceHistoryServiceImpl service;

    User actor;
    Product product;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryServiceImpl(
                priceHistoryRepository, productRepository,
                warehouseRepository, userRepository, notificationRepository, auditUtil,
                accountingPeriodService, userWarehouseAssignmentRepository);

        actor = new User();
        actor.setId(1L);
        actor.setFullName("Kế toán viên A");
        actor.setRole(UserRole.ACCOUNTANT);
        lenient().when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L));

        product = new Product();
        product.setId(10L);
        product.setSku("POT-001");
        product.setName("Nồi inox");

        warehouse = new Warehouse();
        warehouse.setId(1L);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_returnsPending() {
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 7, 1));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findConflictingActive(eq(10L), anyLong(), any(), isNull()))
                .thenReturn(List.of());
        when(userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER)).thenReturn(List.of());
        PriceHistory saved = pendingPriceHistory(1L);
        when(priceHistoryRepository.saveAndFlush(any())).thenReturn(saved);

        PriceHistoryResponse resp = service.create(req, actor);

        assertThat(resp.getStatus()).isEqualTo("PENDING");
        verify(notificationRepository, never()).save(any()); // no managers to notify
    }

    @Test
    void create_effectiveDateInClosedPeriod_throws() {
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 1, 1));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        doThrow(new com.wms.exception.UnprocessableEntityException("PERIOD_CLOSED: Cannot create or modify transactions in a closed accounting period: 2026-01"))
                .when(accountingPeriodService).validateDateInOpenPeriod(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(com.wms.exception.UnprocessableEntityException.class)
                .hasMessageContaining("PERIOD_CLOSED");
        verify(priceHistoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_conflictsWithApprovedSameEffectiveDate_throws() {
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 7, 1));
        PriceHistory approved = pendingPriceHistory(5L);
        approved.setStatus(PriceHistoryStatus.APPROVED);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findConflictingActive(eq(10L), anyLong(), any(), isNull()))
                .thenReturn(List.of(approved));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void create_conflictsWithExistingPendingSameEffectiveDate_throws() {
        // The fix for a wrong PENDING entry is to edit it, not create a duplicate —
        // so a PENDING entry occupying a date also blocks a new one for that date.
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 7, 1));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findConflictingActive(eq(10L), anyLong(), any(), isNull()))
                .thenReturn(List.of(pendingPriceHistory(5L)));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void create_dbConstraintViolation_throwsOverlappingDate() {
        // Defense-in-depth: findConflictingActive is a SELECT-then-INSERT check and
        // can't be atomic on its own. If a concurrent create slips past it, the DB's
        // uq_price_history_active_effective_date constraint (migration V57) rejects
        // the insert; the service must translate that into the same typed 409
        // instead of letting a raw DataIntegrityViolationException leak out.
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 7, 1));

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(priceHistoryRepository.findConflictingActive(eq(10L), anyLong(), any(), isNull()))
                .thenReturn(List.of());
        when(priceHistoryRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uq_price_history_active_effective_date"));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void create_accountantOutsideAssignedWarehouse_throws() {
        // Accountant is only assigned to warehouse 1L; a request for warehouse 2L
        // (e.g. a different accountant's kho) must be rejected, not silently allowed.
        PriceHistoryCreateRequest req = buildCreateRequest(LocalDate.of(2026, 7, 1));
        req.setWarehouseId(2L);
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(otherWarehouse));

        assertThatThrownBy(() -> service.create(req, actor))
                .isInstanceOf(AccessDeniedException.class);
        verify(priceHistoryRepository, never()).saveAndFlush(any());
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_pending_setsStatusCancelled() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setCreatedBy(actor);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(priceHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PriceHistoryResponse resp = service.cancel(1L, actor);

        assertThat(resp.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_approved_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        ph.setCreatedBy(actor);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.cancel(1L, actor))
                .isInstanceOf(PriceHistoryException.class)
                .hasMessageContaining("duyệt");
    }

    @Test
    void cancel_byDifferentUser_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        User other = new User();
        other.setId(99L);
        ph.setCreatedBy(other);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.cancel(1L, actor))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_pending_setsApproved() {
        PriceHistory ph = pendingPriceHistory(1L);
        User manager = new User();
        manager.setId(2L);
        manager.setFullName("KTT");
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));
        when(priceHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PriceHistoryResponse resp = service.approve(1L, manager);

        assertThat(resp.getStatus()).isEqualTo("APPROVED");
        // Overlap is only checked at creation/update now (not re-checked at approval),
        // so approve() must not query for conflicting active entries.
        verify(priceHistoryRepository, never()).findConflictingActive(any(), any(), any(), any());
    }

    @Test
    void approve_alreadyApproved_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        User manager = new User();
        manager.setId(2L);
        manager.setFullName("KTT");

        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.approve(1L, manager))
                .isInstanceOf(PriceHistoryException.class);
    }

    // ── lookupApproved ────────────────────────────────────────────────────────

    @Test
    void lookupApproved_found_returnsLatestPriceAtOrBeforeDate() {
        PriceHistory ph = pendingPriceHistory(1L);
        ph.setStatus(PriceHistoryStatus.APPROVED);
        when(priceHistoryRepository
                .findFirstByProductIdAndWarehouseIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescApprovedAtDesc(
                        eq(10L), anyLong(), eq(PriceHistoryStatus.APPROVED), eq(LocalDate.of(2026, 6, 15))))
                .thenReturn(Optional.of(ph));

        Optional<PriceHistory> result = service.lookupApproved(10L, 1L, LocalDate.of(2026, 6, 15));

        assertThat(result).isPresent();
    }

    @Test
    void lookupApproved_notFound_returnsEmpty() {
        when(priceHistoryRepository
                .findFirstByProductIdAndWarehouseIdAndStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescApprovedAtDesc(
                        eq(10L), anyLong(), eq(PriceHistoryStatus.APPROVED), eq(LocalDate.of(2026, 7, 1))))
                .thenReturn(Optional.empty());

        Optional<PriceHistory> result = service.lookupApproved(10L, 1L, LocalDate.of(2026, 7, 1));

        assertThat(result).isEmpty();
    }

    // ── warehouse scope (getById / getAll) ──────────────────────────────────────

    @Test
    void getById_accountantOutsideAssignedWarehouse_throws() {
        PriceHistory ph = pendingPriceHistory(1L);
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        ph.setWarehouse(otherWarehouse);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        assertThatThrownBy(() -> service.getById(1L, actor))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_nullActor_bypassesScopeCheck() {
        // Internal callers (e.g. the /lookup preview endpoint) pass a null actor and
        // must not be blocked by warehouse scoping.
        PriceHistory ph = pendingPriceHistory(1L);
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        ph.setWarehouse(otherWarehouse);
        when(priceHistoryRepository.findById(1L)).thenReturn(Optional.of(ph));

        PriceHistoryResponse resp = service.getById(1L, null);

        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void getAll_accountantRequestsWarehouseOutsideScope_throws() {
        assertThatThrownBy(() -> service.getAll(null, 2L, null, null, null, actor))
                .isInstanceOf(AccessDeniedException.class);
        verify(priceHistoryRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void getAll_accountantNoWarehouseFilter_stillQueriesRestrictedToAssignedWarehouses() {
        when(priceHistoryRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        List<PriceHistoryResponse> result = service.getAll(null, null, null, null, null, actor);

        assertThat(result).isEmpty();
        verify(priceHistoryRepository).findAll(any(Specification.class), any(Sort.class));
    }

    // ── warehouse scope (getByProduct) ──────────────────────────────────────────

    @Test
    void getByProduct_noWarehouseFilter_returnsEntriesFromAssignedWarehouseOnly() {
        // Regression test: the "Lịch sử giá" modal was showing entries from every
        // warehouse for a product even though the accountant is only assigned to
        // warehouse 1L — entries from an unassigned warehouse 2L must be excluded.
        PriceHistory ownWarehouseEntry = pendingPriceHistory(1L);
        PriceHistory otherWarehouseEntry = pendingPriceHistory(2L);
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        otherWarehouseEntry.setWarehouse(otherWarehouse);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(priceHistoryRepository.findByProductIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(ownWarehouseEntry, otherWarehouseEntry));

        ProductPriceHistoryResponse resp = service.getByProduct(10L, null, actor);

        assertThat(resp.getEntries()).extracting(PriceHistoryResponse::getId).containsExactly(1L);
    }

    @Test
    void getByProduct_explicitWarehouseOutsideScope_throws() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getByProduct(10L, 2L, actor))
                .isInstanceOf(AccessDeniedException.class);
        verify(priceHistoryRepository, never()).findByProductIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getByProduct_nonAccountantRole_seesAllWarehouses() {
        PriceHistory ownWarehouseEntry = pendingPriceHistory(1L);
        PriceHistory otherWarehouseEntry = pendingPriceHistory(2L);
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        otherWarehouseEntry.setWarehouse(otherWarehouse);
        User manager = new User();
        manager.setId(2L);
        manager.setRole(UserRole.ACCOUNTANT_MANAGER);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(priceHistoryRepository.findByProductIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(ownWarehouseEntry, otherWarehouseEntry));

        ProductPriceHistoryResponse resp = service.getByProduct(10L, null, manager);

        assertThat(resp.getEntries()).hasSize(2);
    }

    // ── import (warehouse override / scope) ─────────────────────────────────────

    @Test
    void importFromExcel_withTargetWarehouseOverride_ignoresFileWarehouseCode() throws Exception {
        // Regression test: re-importing a file exported from warehouse "HP-01" while
        // targeting a different warehouse must apply prices to the target, not silently
        // re-target the original warehouse from the file's warehouse_code column.
        Warehouse targetWarehouse = new Warehouse();
        targetWarehouse.setId(2L);
        targetWarehouse.setCode("HN-01");
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L, 2L));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(targetWarehouse));
        when(productRepository.findBySkuAndIsActiveTrue("POT-001")).thenReturn(Optional.of(product));
        when(priceHistoryRepository.findConflictingActive(eq(10L), eq(2L), any(), isNull())).thenReturn(List.of());
        when(priceHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile file = buildImportExcel(List.<String[]>of(
                new String[] { "POT-001", "HP-01", "01/07/2026", "80000", "115000", "" }
        ));

        PriceImportResponse resp = service.importFromExcel(file, 2L, actor);

        assertThat(resp.getCreatedCount()).isEqualTo(1);
        assertThat(resp.getFailedCount()).isEqualTo(0);
        verify(warehouseRepository, never()).findByCode(any());
    }

    @Test
    void importFromExcel_targetWarehouseOutsideAccountantScope_throws() throws Exception {
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        otherWarehouse.setCode("HN-01");
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(otherWarehouse));

        MockMultipartFile file = buildImportExcel(List.<String[]>of(
                new String[] { "POT-001", "HP-01", "01/07/2026", "80000", "115000", "" }
        ));

        assertThatThrownBy(() -> service.importFromExcel(file, 2L, actor))
                .isInstanceOf(AccessDeniedException.class);
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void importFromExcel_noOverride_rowWarehouseOutsideAccountantScope_failsRowNotThrows() throws Exception {
        // No override: warehouse_code drives the target per row, but an ACCOUNTANT still
        // can't bulk-import into a warehouse they aren't assigned to (assigned: {1L} only).
        Warehouse otherWarehouse = new Warehouse();
        otherWarehouse.setId(2L);
        otherWarehouse.setCode("HN-01");
        when(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(1L)).thenReturn(List.of(1L));
        when(productRepository.findBySkuAndIsActiveTrue("POT-001")).thenReturn(Optional.of(product));
        when(warehouseRepository.findByCode("HN-01")).thenReturn(Optional.of(otherWarehouse));

        MockMultipartFile file = buildImportExcel(List.<String[]>of(
                new String[] { "POT-001", "HN-01", "01/07/2026", "80000", "115000", "" }
        ));

        PriceImportResponse resp = service.importFromExcel(file, null, actor);

        assertThat(resp.getCreatedCount()).isEqualTo(0);
        assertThat(resp.getFailedCount()).isEqualTo(1);
        assertThat(resp.getFailed().get(0).getErrorCode()).isEqualTo("WAREHOUSE_SCOPE_FORBIDDEN");
        verify(priceHistoryRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MockMultipartFile buildImportExcel(List<String[]> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("price_import");
            Row header = sheet.createRow(0);
            String[] cols = { "product_sku", "warehouse_code", "effective_date", "cost_price", "selling_price", "notes" };
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            int rowIdx = 1;
            for (String[] row : rows) {
                Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.length; i++) r.createCell(i).setCellValue(row[i]);
            }
            wb.write(out);
            return new MockMultipartFile("file", "price_import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private PriceHistoryCreateRequest buildCreateRequest(LocalDate effective) {
        PriceHistoryCreateRequest req = new PriceHistoryCreateRequest();
        req.setProductId(10L);
        req.setWarehouseId(1L);
        req.setEffectiveDate(effective);
        req.setCostPrice(new BigDecimal("80000"));
        req.setSellingPrice(new BigDecimal("115000"));
        return req;
    }

    private PriceHistory pendingPriceHistory(Long id) {
        PriceHistory ph = new PriceHistory();
        ph.setId(id);
        ph.setProduct(product);
        ph.setWarehouse(warehouse);
        ph.setEffectiveDate(LocalDate.of(2026, 7, 1));
        ph.setCostPrice(new BigDecimal("80000"));
        ph.setSellingPrice(new BigDecimal("115000"));
        ph.setStatus(PriceHistoryStatus.PENDING);
        ph.setCreatedBy(actor);
        ph.setCreatedAt(OffsetDateTime.now());
        ph.setUpdatedAt(OffsetDateTime.now());
        return ph;
    }
}
