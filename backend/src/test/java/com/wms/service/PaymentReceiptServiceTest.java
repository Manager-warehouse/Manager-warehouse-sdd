package com.wms.service;

import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.service.impl.PaymentReceiptServiceImpl;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private SystemConfigRepository systemConfigRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private PaymentReceiptServiceImpl paymentReceiptService;

    private User accountantUser;
    private User storekeeperUser;
    private Dealer dealer;
    private Invoice invoice;
    private DeliveryOrder deliveryOrder;
    private Warehouse warehouse;
    private AccountingPeriod period;
    private DocumentSequence sequence;

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

        dealer = new Dealer();
        dealer.setId(10L);
        dealer.setCode("DL-001");
        dealer.setName("Dai Ly A");
        dealer.setCurrentBalance(BigDecimal.valueOf(30000000));
        dealer.setCreditLimit(BigDecimal.valueOf(50000000));
        dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);

        warehouse = new Warehouse();
        warehouse.setId(5L);

        deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(20L);
        deliveryOrder.setWarehouse(warehouse);

        invoice = new Invoice();
        invoice.setId(50L);
        invoice.setInvoiceNumber("INV-202606-000100");
        invoice.setDealer(dealer);
        invoice.setTotalAmount(BigDecimal.valueOf(20000000));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setDeliveryOrder(deliveryOrder);

        period = new AccountingPeriod();
        period.setId(30L);
        period.setPeriodName("Kỳ 06-2026");
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(AccountingPeriodStatus.OPEN);

        sequence = new DocumentSequence();
        sequence.setSequenceKey("PAYMENT");
        sequence.setNextValue(200L);
    }

    @Test
    @DisplayName("Ghi nhận phiếu thu thành công và chuyển trạng thái hóa đơn sang PAID + Mở khóa tín dụng")
    void createPaymentReceipt_success_fullyPaid_unlocksCredit() {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(20000000));
        request.setPaymentDate(LocalDate.of(2026, 6, 20));
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        request.setNotes("Paying fully");

        SystemConfig bufferConfig = new SystemConfig();
        bufferConfig.setConfigKey("CREDIT_UNLOCK_BUFFER_PCT");
        bufferConfig.setConfigValue("0.8");

        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));
        when(paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getPaymentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(period));
        when(systemConfigRepository.findByConfigKey("CREDIT_UNLOCK_BUFFER_PCT")).thenReturn(Optional.of(bufferConfig));
        when(sequenceRepository.findBySequenceKeyForUpdate("PAYMENT")).thenReturn(Optional.of(sequence));

        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(invocation -> {
            PaymentReceipt pr = invocation.getArgument(0);
            pr.setId(60L);
            return pr;
        });

        PaymentReceiptResponse response = paymentReceiptService.createPaymentReceipt(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentNumber()).isEqualTo("PAY-202606-000200");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(10000000));
        // Threshold unlock: 50,000,000 * 0.8 = 40,000,000. Balance 10,000,000 < 40,000,000, credit status should change to ACTIVE
        assertThat(dealer.getCreditStatus()).isEqualTo(CreditStatus.ACTIVE);

        verify(accountingPeriodService).validateDateInOpenPeriod(request.getPaymentDate());
        verify(invoiceRepository).save(invoice);
        verify(dealerRepository).save(dealer);
        verify(auditLogService).log(eq(accountantUser), eq(AuditAction.CREATE), eq("PAYMENT_RECEIPT"),
                eq(60L), eq("PAY-202606-000200"), eq(5L), any(), any());
    }

    @Test
    @DisplayName("Ghi nhận phiếu thu thất bại - Quyền truy cập không hợp lệ")
    void createPaymentReceipt_deniedForNonAccountant() {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(10000000));
        request.setPaymentDate(LocalDate.of(2026, 6, 20));

        assertThatThrownBy(() -> paymentReceiptService.createPaymentReceipt(request, storekeeperUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied: Accountant role required");
    }

    @Test
    @DisplayName("Ghi nhận phiếu thu thất bại - Hóa đơn đã thanh toán xong")
    void createPaymentReceipt_invoiceAlreadyPaid() {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(5000000));
        request.setPaymentDate(LocalDate.of(2026, 6, 20));

        invoice.setStatus(InvoiceStatus.PAID);

        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> paymentReceiptService.createPaymentReceipt(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Invoice is already fully paid");
    }

    @Test
    @DisplayName("Ghi nhận phiếu thu thất bại - Số tiền vượt quá số dư hóa đơn")
    void createPaymentReceipt_amountExceedsRemaining() {
        PaymentReceiptCreateRequest request = new PaymentReceiptCreateRequest();
        request.setDealerId(10L);
        request.setInvoiceId(50L);
        request.setAmount(BigDecimal.valueOf(25000000)); // Hóa đơn chỉ nợ 20000000
        request.setPaymentDate(LocalDate.of(2026, 6, 20));

        when(dealerRepository.findById(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));
        when(paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentReceiptService.createPaymentReceipt(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Payment amount exceeds invoice remaining balance");
    }
}
