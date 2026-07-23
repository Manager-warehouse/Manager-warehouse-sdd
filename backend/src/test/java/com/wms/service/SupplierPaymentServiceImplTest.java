package com.wms.service;

import com.wms.dto.request.CreateSupplierPaymentRequest;
import com.wms.dto.response.SupplierPaymentOcrResponse;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.billing_payment.PaymentMethod;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.impl.SupplierPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierPaymentServiceImplTest {

    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private SupplierPaymentServiceImpl supplierPaymentService;

    private User accountantUser;
    private Supplier supplier;
    private SupplierInvoice invoice;
    private AccountingPeriod openPeriod;

    @BeforeEach
    void setUp() {
        accountantUser = new User();
        accountantUser.setId(1L);
        accountantUser.setFullName("Ke Toan Vien");
        accountantUser.setRole(UserRole.ACCOUNTANT);

        supplier = new Supplier();
        supplier.setId(10L);
        supplier.setCode("SUP-001");
        supplier.setCompanyName("Nha Cung Cap A");
        supplier.setCurrentBalance(new BigDecimal("50000000.00"));

        invoice = new SupplierInvoice();
        invoice.setId(50L);
        invoice.setInvoiceNumber("SINV-202607-000001");
        invoice.setSupplierInvoiceNumber("VAT-NCC-001");
        invoice.setSupplier(supplier);
        invoice.setTotalAmount(new BigDecimal("50000000.00"));
        invoice.setStatus(InvoiceStatus.UNPAID);

        openPeriod = new AccountingPeriod();
        openPeriod.setId(5L);
        openPeriod.setStatus(AccountingPeriodStatus.OPEN);
    }

    @Test
    @DisplayName("Lập phiếu chi thanh toán cho NCC thành công")
    void createSupplierPayment_success() {
        CreateSupplierPaymentRequest request = CreateSupplierPaymentRequest.builder()
                .supplierId(10L)
                .supplierInvoiceId(50L)
                .amount(new BigDecimal("20000000.00"))
                .paymentDate(LocalDate.of(2026, 7, 23))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .documentDate(LocalDate.of(2026, 7, 23))
                .build();

        DocumentSequence sequence = new DocumentSequence();
        sequence.setSequenceKey("SUPPLIER_PAYMENT");
        sequence.setNextValue(1L);

        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(supplierInvoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));
        when(supplierPaymentRepository.findBySupplierInvoiceId(50L)).thenReturn(Collections.emptyList());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(openPeriod));
        when(sequenceRepository.findBySequenceKeyForUpdate(anyString())).thenReturn(Optional.of(sequence));
        when(supplierPaymentRepository.save(any(SupplierPayment.class))).thenAnswer(i -> {
            SupplierPayment sp = i.getArgument(0);
            sp.setId(100L);
            return sp;
        });

        SupplierPaymentResponse response = supplierPaymentService.createSupplierPayment(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("20000000.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(supplier.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("30000000.00"));
    }

    @Test
    @DisplayName("Quét OCR Ủy nhiệm chi NCC thành công")
    void scanSupplierPaymentOcr_success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unc_25000000_nhacungcapa.jpg",
                "image/jpeg",
                "dummy content".getBytes()
        );

        when(supplierRepository.findAll()).thenReturn(Collections.singletonList(supplier));

        SupplierPaymentOcrResponse response = supplierPaymentService.scanSupplierPaymentOcr(file, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("25000000"));
        assertThat(response.getSupplierId()).isEqualTo(10L);
        assertThat(response.getConfidenceScore()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("Quét OCR thất bại khi file rỗng")
    void scanSupplierPaymentOcr_emptyFileFails() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> supplierPaymentService.scanSupplierPaymentOcr(emptyFile, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("empty");
    }
}
