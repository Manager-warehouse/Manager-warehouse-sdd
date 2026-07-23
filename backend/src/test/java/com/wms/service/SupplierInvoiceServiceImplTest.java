package com.wms.service;

import com.wms.dto.request.CreateSupplierInvoiceRequest;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.stock_receiving.ReceiptStatus;
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
    @Mock private SupplierRepository supplierRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private SupplierBillingNotificationRepository supplierBillingNotificationRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;

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
        receipt.setStatus(ReceiptStatus.APPROVED);
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

        when(receiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(supplierInvoiceRepository.findByReceiptId(100L)).thenReturn(Optional.empty());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(openPeriod));
        when(sequenceRepository.findBySequenceKeyForUpdate(anyString())).thenReturn(Optional.of(sequence));
        when(supplierInvoiceRepository.save(any(SupplierInvoice.class))).thenAnswer(i -> {
            SupplierInvoice inv = i.getArgument(0);
            inv.setId(500L);
            return inv;
        });

        SupplierInvoiceResponse response = supplierInvoiceService.createSupplierInvoice(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getSupplierInvoiceNumber()).isEqualTo("VAT-NCC-001");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(supplierInvoiceRepository).save(any(SupplierInvoice.class));
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
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("APPROVED status");
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
