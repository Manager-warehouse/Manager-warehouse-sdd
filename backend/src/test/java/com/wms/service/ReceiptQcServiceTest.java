package com.wms.service;

import com.wms.dto.request.*;
import com.wms.dto.response.ReceiptDetailResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.repository.*;
import com.wms.service.impl.ReceiptInventoryHelper;
import com.wms.service.impl.ReceiptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 12 test cases cho US-WMS-03: QC Inbound theo Sample.
 * TC01–TC09: PASS ✅  |  TC10–TC12: FAIL ❌ (intentional wrong assertion)
 */
@ExtendWith(MockitoExtension.class)
class ReceiptQcServiceTest {

    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private DebitNoteRepository debitNoteRepository;
    @Mock private ReceiptInventoryHelper inventoryHelper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    private User actor;
    private Warehouse warehouse;
    private Supplier supplier;
    private Product product;
    private Receipt draftReceipt;
    private ReceiptItem receiptItem;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setRole(UserRole.WAREHOUSE_STAFF);

        warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Kho Hải Phòng");

        supplier = new Supplier();
        supplier.setId(5L);
        supplier.setCompanyName("NCC Test");

        product = new Product();
        product.setId(100L);
        product.setSku("SKU-001");
        product.setName("Xoong inox 22cm");
        product.setIsActive(true);

        draftReceipt = new Receipt();
        draftReceipt.setId(1L);
        draftReceipt.setReceiptNumber("REC-202506-00001");
        draftReceipt.setStatus(ReceiptStatus.DRAFT);
        draftReceipt.setWarehouse(warehouse);
        draftReceipt.setSupplier(supplier);
        draftReceipt.setDocumentDate(LocalDate.now());
        draftReceipt.setType(ReceiptType.PURCHASE);
        draftReceipt.setCreatedAt(OffsetDateTime.now());
        draftReceipt.setUpdatedAt(OffsetDateTime.now());

        receiptItem = new ReceiptItem();
        receiptItem.setId(200L);
        receiptItem.setProduct(product);
        receiptItem.setExpectedQty(new BigDecimal("100"));
        receiptItem.setActualQty(new BigDecimal("100"));
        receiptItem.setQcSamplingMethod(QcSamplingMethod.RANDOM_SAMPLE);
        receiptItem.setQcResult(QcResult.PENDING);
    }

    // =========================================================================
    // TC01 — Tạo phiếu nhập thành công → PENDING_RECEIPT ✅
    // =========================================================================
    @Test
    @DisplayName("TC01 ✅ Planner tạo phiếu nhập → status PENDING_RECEIPT, receipt_number được sinh")
    void TC01_createReceipt_success() {
        ReceiptCreateRequest req = new ReceiptCreateRequest();
        req.setSupplierId(5L);
        req.setWarehouseId(10L);
        req.setContactPerson("Nguyễn Văn A");
        req.setSourceOrderCode("PO-2025-001");
        req.setSourceChannel(SourceChannel.ZALO);
        req.setDocumentDate(LocalDate.now());
        ReceiptCreateItemRequest item = new ReceiptCreateItemRequest();
        item.setProductId(100L);
        item.setExpectedQty(new BigDecimal("500"));
        req.setItems(List.of(item));

        Receipt savedReceipt = new Receipt();
        savedReceipt.setId(1L);
        savedReceipt.setReceiptNumber("TEMP");
        savedReceipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
        savedReceipt.setWarehouse(warehouse);
        savedReceipt.setSupplier(supplier);
        savedReceipt.setDocumentDate(LocalDate.now());
        savedReceipt.setType(ReceiptType.PURCHASE);
        savedReceipt.setCreatedAt(OffsetDateTime.now());
        savedReceipt.setUpdatedAt(OffsetDateTime.now());

        when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(receiptRepository.existsBySourceOrderCodeAndSupplierIdAndWarehouseId(any(), any(), any())).thenReturn(false);
        when(receiptRepository.save(any())).thenReturn(savedReceipt);
        when(productRepository.findByIdAndIsActiveTrue(100L)).thenReturn(Optional.of(product));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));

        ReceiptDetailResponse result = receiptService.create(req, actor);

        assertThat(result.getStatus()).isEqualTo("PENDING_RECEIPT");
        verify(receiptRepository, times(2)).save(any(Receipt.class));
    }

    // =========================================================================
    // TC02 — Nhận hàng thực tế → status DRAFT ✅
    // =========================================================================
    @Test
    @DisplayName("TC02 ✅ Warehouse Staff ghi actual_qty → status chuyển sang DRAFT")
    void TC02_receive_updatesActualQty_and_setsDraft() {
        Receipt pendingReceipt = new Receipt();
        pendingReceipt.setId(1L);
        pendingReceipt.setReceiptNumber("REC-202506-00001");
        pendingReceipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
        pendingReceipt.setWarehouse(warehouse);
        pendingReceipt.setSupplier(supplier);
        pendingReceipt.setDocumentDate(LocalDate.now());
        pendingReceipt.setType(ReceiptType.PURCHASE);
        pendingReceipt.setCreatedAt(OffsetDateTime.now());
        pendingReceipt.setUpdatedAt(OffsetDateTime.now());

        ReceiptReceiveItemRequest itemReq = new ReceiptReceiveItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setActualQty(new BigDecimal("98"));

        ReceiptReceiveRequest req = new ReceiptReceiveRequest();
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(pendingReceipt));
        when(receiptRepository.countBySupplierIdAndStatus(5L, ReceiptStatus.APPROVED)).thenReturn(3L);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReceiptDetailResponse result = receiptService.receive(1L, req, actor);

        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(receiptItem.getActualQty()).isEqualByComparingTo("98");
    }

    // =========================================================================
    // TC03 — Supplier < 5 approved receipts → FULL_INSPECTION ✅
    // =========================================================================
    @Test
    @DisplayName("TC03 ✅ Supplier mới (< 5 APPROVED) → qc_sampling_method = FULL_INSPECTION")
    void TC03_receive_newSupplier_setsFullInspection() {
        Receipt pendingReceipt = new Receipt();
        pendingReceipt.setId(1L);
        pendingReceipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
        pendingReceipt.setWarehouse(warehouse);
        pendingReceipt.setSupplier(supplier);
        pendingReceipt.setDocumentDate(LocalDate.now());
        pendingReceipt.setType(ReceiptType.PURCHASE);
        pendingReceipt.setReceiptNumber("REC-202506-00001");
        pendingReceipt.setCreatedAt(OffsetDateTime.now());
        pendingReceipt.setUpdatedAt(OffsetDateTime.now());

        ReceiptReceiveItemRequest itemReq = new ReceiptReceiveItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setActualQty(new BigDecimal("100"));

        ReceiptReceiveRequest req = new ReceiptReceiveRequest();
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(pendingReceipt));
        when(receiptRepository.countBySupplierIdAndStatus(5L, ReceiptStatus.APPROVED)).thenReturn(2L);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        receiptService.receive(1L, req, actor);

        assertThat(receiptItem.getQcSamplingMethod()).isEqualTo(QcSamplingMethod.FULL_INSPECTION);
    }

    // =========================================================================
    // TC04 — Supplier ≥ 5 approved receipts → RANDOM_SAMPLE ✅
    // =========================================================================
    @Test
    @DisplayName("TC04 ✅ Supplier ≥ 5 APPROVED → qc_sampling_method = RANDOM_SAMPLE")
    void TC04_receive_establishedSupplier_setsRandomSample() {
        Receipt pendingReceipt = new Receipt();
        pendingReceipt.setId(1L);
        pendingReceipt.setStatus(ReceiptStatus.PENDING_RECEIPT);
        pendingReceipt.setWarehouse(warehouse);
        pendingReceipt.setSupplier(supplier);
        pendingReceipt.setDocumentDate(LocalDate.now());
        pendingReceipt.setType(ReceiptType.PURCHASE);
        pendingReceipt.setReceiptNumber("REC-202506-00001");
        pendingReceipt.setCreatedAt(OffsetDateTime.now());
        pendingReceipt.setUpdatedAt(OffsetDateTime.now());

        ReceiptReceiveItemRequest itemReq = new ReceiptReceiveItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setActualQty(new BigDecimal("100"));

        ReceiptReceiveRequest req = new ReceiptReceiveRequest();
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(pendingReceipt));
        when(receiptRepository.countBySupplierIdAndStatus(5L, ReceiptStatus.APPROVED)).thenReturn(7L);
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        receiptService.receive(1L, req, actor);

        assertThat(receiptItem.getQcSamplingMethod()).isEqualTo(QcSamplingMethod.RANDOM_SAMPLE);
    }

    // =========================================================================
    // TC05 — QC Submit thành công, sample data được ghi ✅
    // =========================================================================
    @Test
    @DisplayName("TC05 ✅ QC Submit hợp lệ → sample_qty, passed, failed được lưu vào receipt_item")
    void TC05_qcSubmit_valid_savesSampleData() {
        ReceiptQcItemRequest itemReq = new ReceiptQcItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setSampleQty(new BigDecimal("10"));
        itemReq.setSamplePassedQty(new BigDecimal("9"));
        itemReq.setSampleFailedQty(new BigDecimal("1"));
        itemReq.setQcFailureReason("Ngoại quan xấu");

        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("SUBMIT");
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(inventoryHelper.computeItemQcResult(any(), any(), any())).thenReturn(QcResult.PARTIAL);

        receiptService.qc(1L, req, actor);

        assertThat(receiptItem.getSampleQty()).isEqualByComparingTo("10");
        assertThat(receiptItem.getSamplePassedQty()).isEqualByComparingTo("9");
        assertThat(receiptItem.getSampleFailedQty()).isEqualByComparingTo("1");
        assertThat(receiptItem.getQcFailureReason()).isEqualTo("Ngoại quan xấu");
    }

    // =========================================================================
    // TC06 — Tất cả mẫu đạt → QcResult = PASSED ✅
    // =========================================================================
    @Test
    @DisplayName("TC06 ✅ Sample 10 passed, 0 failed → QcResult = PASSED")
    void TC06_qcSubmit_allPassed_setsQcResultPassed() {
        ReceiptQcItemRequest itemReq = new ReceiptQcItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setSampleQty(new BigDecimal("10"));
        itemReq.setSamplePassedQty(new BigDecimal("10"));
        itemReq.setSampleFailedQty(BigDecimal.ZERO);

        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("SUBMIT");
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(inventoryHelper.computeItemQcResult(any(), eq(new BigDecimal("10")), eq(BigDecimal.ZERO)))
                .thenReturn(QcResult.PASSED);

        receiptService.qc(1L, req, actor);

        assertThat(receiptItem.getQcResult()).isEqualTo(QcResult.PASSED);
    }

    // =========================================================================
    // TC07 — Mẫu hỗn hợp → QcResult = PARTIAL ✅
    // =========================================================================
    @Test
    @DisplayName("TC07 ✅ Sample 10: 9 passed, 1 failed → QcResult = PARTIAL")
    void TC07_qcSubmit_mixed_setsQcResultPartial() {
        ReceiptQcItemRequest itemReq = new ReceiptQcItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setSampleQty(new BigDecimal("10"));
        itemReq.setSamplePassedQty(new BigDecimal("9"));
        itemReq.setSampleFailedQty(new BigDecimal("1"));

        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("SUBMIT");
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(inventoryHelper.computeItemQcResult(any(), any(), any())).thenReturn(QcResult.PARTIAL);

        receiptService.qc(1L, req, actor);

        assertThat(receiptItem.getQcResult()).isEqualTo(QcResult.PARTIAL);
    }

    // =========================================================================
    // TC08 — Storekeeper confirm PASSED → QC_COMPLETED ✅
    // =========================================================================
    @Test
    @DisplayName("TC08 ✅ Storekeeper confirm PASSED → receipt status = QC_COMPLETED")
    void TC08_qcConfirm_passed_statusBecomesQcCompleted() {
        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("CONFIRM");
        req.setDecision("PASSED");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReceiptDetailResponse result = receiptService.qc(1L, req, actor);

        assertThat(result.getStatus()).isEqualTo("QC_COMPLETED");
        verify(inventoryHelper, never()).upsertQuarantineInventory(any(), any(), any(), any(), any());
    }

    // =========================================================================
    // TC09 — Storekeeper confirm FAILED → QC_FAILED + quarantine inventory ✅
    // =========================================================================
    @Test
    @DisplayName("TC09 ✅ Storekeeper confirm FAILED → QC_FAILED + quarantine inventory cộng actual_qty")
    void TC09_qcConfirm_failed_statusQcFailed_and_quarantineCreated() {
        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("CONFIRM");
        req.setDecision("FAILED");

        WarehouseLocation quarantineLoc = new WarehouseLocation();
        quarantineLoc.setId(99L);
        quarantineLoc.setIsQuarantine(true);

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));
        when(inventoryHelper.requireQuarantineLocation(10L)).thenReturn(quarantineLoc);
        when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReceiptDetailResponse result = receiptService.qc(1L, req, actor);

        assertThat(result.getStatus()).isEqualTo("QC_FAILED");
        verify(inventoryHelper).upsertQuarantineInventory(
                eq(warehouse), eq(product), eq(quarantineLoc),
                eq(new BigDecimal("100")), isNull());
    }

    // =========================================================================
    // TC10 — ❌ FAIL: sample mismatch nhưng test assert không có exception
    // Thực tế: passed(9) + failed(2) ≠ sample(10) → BusinessRuleViolationException
    // Test này sẽ FAIL vì assertion sai: không expect exception
    // =========================================================================
    @Test
    @DisplayName("TC10 ❌ [INTENTIONAL FAIL] QC Submit: passed+failed≠sample_qty — test sai khi không expect exception")
    void TC10_FAIL_qcSubmit_sampleMismatch_wrongAssertion() {
        ReceiptQcItemRequest itemReq = new ReceiptQcItemRequest();
        itemReq.setReceiptItemId(200L);
        itemReq.setSampleQty(new BigDecimal("10"));
        itemReq.setSamplePassedQty(new BigDecimal("9"));
        itemReq.setSampleFailedQty(new BigDecimal("2")); // 9+2=11 ≠ 10 → sẽ throw

        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("SUBMIT");
        req.setItems(List.of(itemReq));

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(draftReceipt));
        when(receiptItemRepository.findByReceiptId(1L)).thenReturn(List.of(receiptItem));

        // BUG TRONG TEST: không expect exception → test sẽ FAIL với BusinessRuleViolationException
        ReceiptDetailResponse result = receiptService.qc(1L, req, actor);
        assertThat(result).isNotNull();
    }

    // =========================================================================
    // TC11 — ❌ FAIL: confirm QC trên receipt không ở trạng thái DRAFT
    // Thực tế: receipt là PENDING_RECEIPT → BusinessRuleViolationException
    // Test này sẽ FAIL vì assert status = QC_COMPLETED trong khi code sẽ throw
    // =========================================================================
    @Test
    @DisplayName("TC11 ❌ [INTENTIONAL FAIL] Confirm QC khi receipt không phải DRAFT — test assert sai")
    void TC11_FAIL_qcConfirm_wrongStatus_wrongAssertion() {
        Receipt pendingReceipt = new Receipt();
        pendingReceipt.setId(1L);
        pendingReceipt.setStatus(ReceiptStatus.PENDING_RECEIPT); // sai status
        pendingReceipt.setWarehouse(warehouse);
        pendingReceipt.setSupplier(supplier);
        pendingReceipt.setDocumentDate(LocalDate.now());
        pendingReceipt.setType(ReceiptType.PURCHASE);
        pendingReceipt.setReceiptNumber("REC-202506-00001");
        pendingReceipt.setCreatedAt(OffsetDateTime.now());
        pendingReceipt.setUpdatedAt(OffsetDateTime.now());

        ReceiptQcRequest req = new ReceiptQcRequest();
        req.setAction("CONFIRM");
        req.setDecision("PASSED");

        when(receiptRepository.findById(1L)).thenReturn(Optional.of(pendingReceipt));

        // BUG TRONG TEST: không expect exception → test sẽ FAIL với BusinessRuleViolationException
        ReceiptDetailResponse result = receiptService.qc(1L, req, actor);
        assertThat(result.getStatus()).isEqualTo("QC_COMPLETED");
    }

    // =========================================================================
    // TC12 — ❌ FAIL: tạo phiếu với source trùng nhưng test assert thành công
    // Thực tế: duplicate check → BusinessRuleViolationException
    // Test này sẽ FAIL vì assert status = PENDING_RECEIPT trong khi code sẽ throw
    // =========================================================================
    @Test
    @DisplayName("TC12 ❌ [INTENTIONAL FAIL] Tạo phiếu nhập duplicate sourceOrderCode — test assert sai")
    void TC12_FAIL_createReceipt_duplicateSource_wrongAssertion() {
        ReceiptCreateRequest req = new ReceiptCreateRequest();
        req.setSupplierId(5L);
        req.setWarehouseId(10L);
        req.setContactPerson("Nguyễn Văn B");
        req.setSourceOrderCode("PO-2025-001"); // đã tồn tại
        req.setSourceChannel(SourceChannel.EMAIL);
        req.setDocumentDate(LocalDate.now());
        ReceiptCreateItemRequest item = new ReceiptCreateItemRequest();
        item.setProductId(100L);
        item.setExpectedQty(new BigDecimal("200"));
        req.setItems(List.of(item));

        when(supplierRepository.findById(5L)).thenReturn(Optional.of(supplier));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        // Trả về true = đã tồn tại duplicate → code sẽ throw
        when(receiptRepository.existsBySourceOrderCodeAndSupplierIdAndWarehouseId(
                "PO-2025-001", 5L, 10L)).thenReturn(true);

        // BUG TRONG TEST: không expect exception → test sẽ FAIL với BusinessRuleViolationException
        ReceiptDetailResponse result = receiptService.create(req, actor);
        assertThat(result.getStatus()).isEqualTo("PENDING_RECEIPT");
    }

    // =========================================================================
    // PVS-Studio Demo: 3 private helpers chứa lỗi tĩnh có chủ đích
    // Mỗi method vi phạm một quy tắc mà PVS-Studio Java sẽ báo warning.
    // =========================================================================

    /**
     * PVS-STUDIO ERROR 1 — V6001:
     * Identical sub-expressions on both sides of the '&&' operator.
     * Điều kiện qty != null được kiểm tra hai lần — lần thứ hai thừa và luôn đúng
     * nếu lần đầu đã qua, báo hiệu lỗi copy-paste hoặc logic sai.
     */
    @SuppressWarnings("ConstantConditions")
    private boolean pvsError1_identicalCondition(BigDecimal qty) {
        // V6001: 'qty != null' lặp lại hai lần trong cùng một biểu thức boolean
        return qty != null && qty != null;
    }

    /**
     * PVS-STUDIO ERROR 2 — V6021:
     * The value assigned to 'unusedBatchCode' is never used.
     * Biến được gán giá trị nhưng không bao giờ được đọc hay trả về.
     */
    private int pvsError2_unusedVariable(int receiptId) {
        // V6021: 'unusedBatchCode' được gán nhưng không bao giờ được đọc
        String unusedBatchCode = "REC-202506-DEMO";
        receiptId = receiptId + 1;
        return receiptId;
    }

    /**
     * PVS-STUDIO ERROR 3 — V6047:
     * The condition is always false/true.
     * Biểu thức so sánh luôn trả về cùng một giá trị — logic sai.
     */
    private boolean pvsError3_alwaysFalseCondition(int receiptId) {
        // V6047: receiptId >= 0 && receiptId < 0 luôn là false
        return receiptId >= 0 && receiptId < 0;
    }
}
