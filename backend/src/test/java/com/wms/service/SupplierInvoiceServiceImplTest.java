package com.wms.service;

import com.wms.dto.request.CreateSupplierInvoiceRequest;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.impl.SupplierInvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierInvoiceServiceImplTest {

    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private ReceiptItemRepository receiptItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private SupplierBillingNotificationRepository supplierBillingNotificationRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;

    @InjectMocks
    private SupplierInvoiceServiceImpl supplierInvoiceService;

    private User accountantUser;
    private User storekeeperUser;
    private Supplier supplier;
    private Receipt receipt;
    private AccountingPeriod openPeriod;

    @BeforeEach
    void setUp() {
        accountantUser = new User();
        accountantUser.setId(1L);
        accountantUser.setFullName("Ke Toan Vien");
        accountantUser.setRole(UserRole.ACCOUNTANT);

        storekeeperUser = new User();
        storekeeperUser.setId(2L);
        storekeeperUser.setFullName("Thu Kho");
        storekeeperUser.setRole(UserRole.STOREKEEPER);

        supplier = new Supplier();
        supplier.setId(10L);
        supplier.setCode("SUP-001");
        supplier.setCompanyName("Nha Cung Cap A");
        supplier.setCurrentBalance(BigDecimal.ZERO);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);

        receipt = new Receipt();
        receipt.setId(100L);
        receipt.setReceiptNumber("RO-001");
        receipt.setStatus(ReceiptStatus.PUTAWAY_COMPLETED);
        receipt.setSupplier(supplier);
        receipt.setWarehouse(warehouse);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(5L);
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thành công")
    void createSupplierInvoice_success() {
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        com.wms.entity.document_numbering.DocumentSequence sequence = new com.wms.entity.document_numbering.DocumentSequence();
        sequence.setSequenceKey("SUPPLIER_INVOICE");
        sequence.setNextValue(1L);

        ReceiptItem item1 = new ReceiptItem();
        item1.setActualQty(10);
        item1.setApprovedQty(10);
        item1.setUnitCost(new BigDecimal("50000.00"));
        ReceiptItem item2 = new ReceiptItem();
        item2.setActualQty(5);
        item2.setApprovedQty(5);
        item2.setUnitCost(new BigDecimal("20000.00"));

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(java.util.List.of(item1, item2));
        when(supplierInvoiceRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(openPeriod));
        when(sequenceRepository.findBySequenceKeyForUpdate(anyString())).thenReturn(Optional.of(sequence));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(500L);
            return inv;
        });
        when(supplierPaymentRepository.findBySupplierInvoiceId(500L)).thenReturn(java.util.List.of());

        SupplierInvoiceResponse response = supplierInvoiceService.createSupplierInvoice(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getSupplierInvoiceNumber()).isEqualTo("VAT-NCC-001");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("600000.00");
        assertThat(response.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(supplier.getCurrentBalance()).isEqualByComparingTo("600000.00");
        verify(supplierInvoiceRepository).save(any(SupplierInvoice.class));
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng - Race condition tạo trùng được dịch thành lỗi 409 sạch")
    void createSupplierInvoice_translatesConcurrentDuplicateIntoCleanConflict() {
        // findByReceiptId() is check-then-act, not atomic: a double-click or retried request
        // racing the same receiptId can both pass it before either commits. The DB unique
        // constraint on supplier_invoices.receipt_id (V33) is what actually catches this -
        // simulate that by having save() throw, and assert it's translated into the same clean
        // 409 the sequential (non-race) case above already returns, not a raw 500.
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        ReceiptItem item = new ReceiptItem();
        item.setActualQty(10);
        item.setApprovedQty(10);
        item.setUnitCost(new BigDecimal("50000.00"));

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(java.util.List.of(item));
        when(supplierInvoiceRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(openPeriod));
        com.wms.entity.document_numbering.DocumentSequence sequence = new com.wms.entity.document_numbering.DocumentSequence();
        sequence.setSequenceKey("SUPPLIER_INVOICE");
        sequence.setNextValue(1L);
        when(sequenceRepository.findBySequenceKeyForUpdate(anyString())).thenReturn(Optional.of(sequence));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uq_supplier_invoices_receipt_id"));

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, accountantUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SUPPLIER_INVOICE_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("Xem chi tiết hóa đơn mua hàng trả về đúng số tiền đã thanh toán")
    void getSupplierInvoiceById_reflectsPaidAmountFromExistingPayments() {
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setId(500L);
        invoice.setInvoiceNumber("SINV-202607-000001");
        invoice.setSupplierInvoiceNumber("VAT-NCC-001");
        invoice.setReceipt(receipt);
        invoice.setSupplier(supplier);
        invoice.setTotalAmount(new BigDecimal("600000.00"));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);

        com.wms.entity.billing_payment.SupplierPayment payment = new com.wms.entity.billing_payment.SupplierPayment();
        payment.setAmount(new BigDecimal("200000.00"));

        when(supplierInvoiceRepository.findById(500L)).thenReturn(Optional.of(invoice));
        when(supplierPaymentRepository.findBySupplierInvoiceId(500L)).thenReturn(java.util.List.of(payment));

        SupplierInvoiceResponse response = supplierInvoiceService.getSupplierInvoiceById(500L, accountantUser);

        assertThat(response.getPaidAmount()).isEqualByComparingTo("200000.00");
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thất bại - Thiếu đơn giá nhập trên dòng hàng")
    void createSupplierInvoice_failsWhenUnitCostMissing() {
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        ReceiptItem item = new ReceiptItem();
        item.setActualQty(10);
        item.setApprovedQty(10);
        item.setUnitCost(null);

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(receiptItemRepository.findByReceiptId(100L)).thenReturn(java.util.List.of(item));
        when(supplierInvoiceRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(openPeriod));

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("ITEM_UNIT_COST_MISSING");
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thất bại - Phiếu nhập chưa COMPLETED")
    void createSupplierInvoice_failsWhenReceiptNotCompleted() {
        receipt.setStatus(ReceiptStatus.QC_COMPLETED);

        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, accountantUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("put away");
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thất bại - Số hóa đơn VAT đã được ghi nhận cho NCC này")
    void createSupplierInvoice_failsWhenSupplierInvoiceNumberAlreadyUsed() {
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(supplierInvoiceRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(supplierInvoiceRepository.existsBySupplierIdAndSupplierInvoiceNumber(10L, "VAT-NCC-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, accountantUser))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SUPPLIER_INVOICE_NUMBER_ALREADY_USED");
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thất bại - Hạn thanh toán trước ngày hạch toán")
    void createSupplierInvoice_failsWhenDueDateBeforeDocumentDate() {
        // Validated before any repository lookup, so nothing else needs stubbing here.
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .dueDate(LocalDate.of(2026, 7, 20))
                .build();

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("DUE_DATE_BEFORE_DOCUMENT_DATE");
    }

    @Test
    @DisplayName("Lập hóa đơn mua hàng thất bại - Quyền truy cập không hợp lệ")
    void createSupplierInvoice_failsForNonAccountant() {
        CreateSupplierInvoiceRequest request = CreateSupplierInvoiceRequest.builder()
                .receiptId(100L)
                .supplierInvoiceNumber("VAT-NCC-001")
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        assertThatThrownBy(() -> supplierInvoiceService.createSupplierInvoice(request, storekeeperUser))
                .isInstanceOf(AccessDeniedException.class);
    }
}
