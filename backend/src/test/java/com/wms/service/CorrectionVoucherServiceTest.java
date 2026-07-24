package com.wms.service;

import com.wms.dto.request.CorrectionVoucherCreateRequest;
import com.wms.dto.response.CorrectionVoucherResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.billing_payment.PaymentReceipt;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.stock_control.Adjustment;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.CorrectionVoucherReferenceType;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.dealer_management.CreditStatus;
import com.wms.enums.stock_control.AdjustmentType;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AdjustmentRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PaymentReceiptRepository;
import com.wms.repository.SupplierInvoiceRepository;
import com.wms.repository.SupplierPaymentRepository;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.impl.CorrectionVoucherServiceImpl;
import com.wms.service.user_configuration.SystemConfigService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrectionVoucherServiceTest {

    @Mock private AdjustmentRepository adjustmentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private AuditLogService auditLogService;

    private CorrectionVoucherServiceImpl correctionVoucherService;

    private User accountantManager;
    private User accountant;
    private Dealer dealer;
    private Supplier supplier;
    private Invoice invoice;
    private SupplierInvoice supplierInvoice;
    private AccountingPeriod closedPeriod;
    private AccountingPeriod openPeriod;

    @BeforeEach
    void setUp() {
        correctionVoucherService = new CorrectionVoucherServiceImpl(
                adjustmentRepository, invoiceRepository, paymentReceiptRepository,
                supplierInvoiceRepository, supplierPaymentRepository,
                dealerRepository, supplierRepository, accountingPeriodService,
                systemConfigService, auditLogService);

        accountantManager = new User();
        accountantManager.setId(6L);
        accountantManager.setFullName("Ke Toan Truong");
        accountantManager.setRole(UserRole.ACCOUNTANT_MANAGER);

        accountant = new User();
        accountant.setId(1L);
        accountant.setFullName("Ke Toan Vien");
        accountant.setRole(UserRole.ACCOUNTANT);

        dealer = new Dealer();
        dealer.setId(10L);
        dealer.setCode("DL-001");
        dealer.setName("Dai Ly A");
        dealer.setCurrentBalance(BigDecimal.valueOf(48_000_000));
        dealer.setCreditLimit(BigDecimal.valueOf(500_000_000));
        dealer.setCreditStatus(CreditStatus.ACTIVE);

        supplier = new Supplier();
        supplier.setId(20L);
        supplier.setCode("NCC-001");
        supplier.setCompanyName("Cong ty Gia Dung Phung");
        supplier.setCurrentBalance(BigDecimal.valueOf(45_000_000));

        closedPeriod = new AccountingPeriod();
        closedPeriod.setId(2L);
        closedPeriod.setPeriodName("2026-06");
        closedPeriod.setStatus(AccountingPeriodStatus.CLOSED);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(3L);
        openPeriod.setPeriodName("2026-07");
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);

        invoice = new Invoice();
        invoice.setId(101L);
        invoice.setInvoiceNumber("INV-202606-0005");
        invoice.setDealer(dealer);
        invoice.setTotalAmount(BigDecimal.valueOf(17_000_000));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setAccountingPeriod(closedPeriod);

        supplierInvoice = new SupplierInvoice();
        supplierInvoice.setId(50L);
        supplierInvoice.setInvoiceNumber("SINV-202606-0001");
        supplierInvoice.setSupplier(supplier);
        supplierInvoice.setAccountingPeriod(closedPeriod);
    }

    private CorrectionVoucherCreateRequest requestFor(CorrectionVoucherReferenceType type, Long refId, BigDecimal delta) {
        CorrectionVoucherCreateRequest request = new CorrectionVoucherCreateRequest();
        request.setReferenceType(type);
        request.setReferenceId(refId);
        request.setAmountDelta(delta);
        request.setReason("Hóa đơn ghi nhầm đơn giá, kỳ đã chốt sổ");
        request.setDocumentDate(LocalDate.of(2026, 7, 24));
        return request;
    }

    @Test
    @DisplayName("Lập bút toán điều chỉnh cho hóa đơn thuộc kỳ đã chốt - giảm dư nợ đại lý ngay lập tức")
    void createCorrectionVoucher_invoiceReference_success() {
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 101L, BigDecimal.valueOf(-2_000_000));

        when(invoiceRepository.findById(101L)).thenReturn(Optional.of(invoice));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(accountingPeriodService.resolveOpenPeriod(request.getDocumentDate())).thenReturn(openPeriod);
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> {
            Adjustment adj = invocation.getArgument(0);
            adj.setId(5L);
            return adj;
        });

        CorrectionVoucherResponse response = correctionVoucherService.createCorrectionVoucher(request, accountantManager);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getDealerId()).isEqualTo(10L);
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(46_000_000));

        // Original document must never be touched.
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(17_000_000));
        assertThat(invoice.getAccountingPeriod()).isEqualTo(closedPeriod);

        verify(dealerRepository).save(dealer);
        verify(auditLogService).log(eq(accountantManager), eq(AuditAction.CORRECTION_VOUCHER_CREATE),
                eq("ADJUSTMENT"), eq(5L), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Bút toán điều chỉnh đưa dư nợ vượt hạn mức tín dụng -> tự động CREDIT_HOLD")
    void createCorrectionVoucher_pushesBalanceOverLimit_triggersCreditHold() {
        dealer.setCurrentBalance(BigDecimal.valueOf(480_000_000));
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 101L, BigDecimal.valueOf(30_000_000));

        when(invoiceRepository.findById(101L)).thenReturn(Optional.of(invoice));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(accountingPeriodService.resolveOpenPeriod(request.getDocumentDate())).thenReturn(openPeriod);
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        correctionVoucherService.createCorrectionVoucher(request, accountantManager);

        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(510_000_000));
        assertThat(dealer.getCreditStatus()).isEqualTo(CreditStatus.CREDIT_HOLD);
    }

    @Test
    @DisplayName("Lập bút toán điều chỉnh cho hóa đơn mua hàng NCC - giảm dư nợ phải trả")
    void createCorrectionVoucher_supplierInvoiceReference_success() {
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.SUPPLIER_INVOICE, 50L, BigDecimal.valueOf(-5_000_000));

        when(supplierInvoiceRepository.findById(50L)).thenReturn(Optional.of(supplierInvoice));
        when(supplierRepository.findById(20L)).thenReturn(Optional.of(supplier));
        when(accountingPeriodService.resolveOpenPeriod(request.getDocumentDate())).thenReturn(openPeriod);
        when(adjustmentRepository.save(any(Adjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CorrectionVoucherResponse response = correctionVoucherService.createCorrectionVoucher(request, accountantManager);

        assertThat(response.getSupplierId()).isEqualTo(20L);
        assertThat(supplier.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(40_000_000));
        verify(supplierRepository).save(supplier);
        verifyNoInteractions(dealerRepository);
    }

    @Test
    @DisplayName("Từ chối tạo bút toán điều chỉnh khi kỳ của chứng từ gốc chưa CLOSED")
    void createCorrectionVoucher_originalPeriodStillOpen_rejected() {
        invoice.setAccountingPeriod(openPeriod);
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 101L, BigDecimal.valueOf(-2_000_000));

        when(invoiceRepository.findById(101L)).thenReturn(Optional.of(invoice));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));

        assertThatThrownBy(() -> correctionVoucherService.createCorrectionVoucher(request, accountantManager))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("ORIGINAL_PERIOD_NOT_CLOSED");

        verifyNoInteractions(adjustmentRepository);
    }

    @Test
    @DisplayName("Từ chối tạo bút toán điều chỉnh khi documentDate thuộc kỳ đã CLOSED")
    void createCorrectionVoucher_documentDateInClosedPeriod_rejected() {
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 101L, BigDecimal.valueOf(-2_000_000));

        when(invoiceRepository.findById(101L)).thenReturn(Optional.of(invoice));
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(accountingPeriodService.resolveOpenPeriod(request.getDocumentDate()))
                .thenThrow(new UnprocessableEntityException("PERIOD_CLOSED: Cannot create or modify transactions in a closed accounting period"));

        assertThatThrownBy(() -> correctionVoucherService.createCorrectionVoucher(request, accountantManager))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("PERIOD_CLOSED");

        verifyNoInteractions(adjustmentRepository);
    }

    @Test
    @DisplayName("Từ chối - chứng từ gốc không tồn tại")
    void createCorrectionVoucher_referenceNotFound() {
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 999L, BigDecimal.valueOf(-2_000_000));

        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> correctionVoucherService.createCorrectionVoucher(request, accountantManager))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Từ chối - Kế toán viên (không phải Kế toán trưởng) không có quyền tạo")
    void createCorrectionVoucher_deniedForNonAccountantManager() {
        CorrectionVoucherCreateRequest request = requestFor(
                CorrectionVoucherReferenceType.INVOICE, 101L, BigDecimal.valueOf(-2_000_000));

        assertThatThrownBy(() -> correctionVoucherService.createCorrectionVoucher(request, accountant))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(invoiceRepository);
    }
}
