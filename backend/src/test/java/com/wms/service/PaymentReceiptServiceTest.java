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

import com.wms.dto.request.PaymentReceiptCreateRequest;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.repository.dealer_management.DealerRepository;
import com.wms.service.billing_payment.impl.PaymentReceiptServiceImpl;
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
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private SystemConfigService systemConfigService;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

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

        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));
        when(paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getPaymentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(period));
        when(systemConfigService.getDecimalValue(eq("CREDIT_UNLOCK_BUFFER_PCT"), any()))
                .thenReturn(new BigDecimal("0.8"));
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
    @DisplayName("Job quét cuối ngày khóa tín dụng đại lý quá hạn và cảnh báo Kế toán trưởng (ACC-02)")
    void runDailyOverdueHoldJob_locksDealerAndAlertsAccountantManagers() {
        dealer.setCreditStatus(CreditStatus.ACTIVE);
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setDueDate(LocalDate.now().minusDays(45));

        User manager = new User();
        manager.setId(6L);
        manager.setEmail("acc_manager@phucanh.vn");
        manager.setRole(UserRole.ACCOUNTANT_MANAGER);

        when(invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.UNPAID))
                .thenReturn(List.of(invoice));
        when(invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.PARTIALLY_PAID))
                .thenReturn(Collections.emptyList());
        when(systemConfigService.getIntValue(eq("CREDIT_HOLD_OVERDUE_DAYS"), anyInt())).thenReturn(30);
        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(userRepository.findByRole(UserRole.ACCOUNTANT_MANAGER)).thenReturn(List.of(manager));

        paymentReceiptService.runDailyOverdueHoldJob();

        assertThat(dealer.getCreditStatus()).isEqualTo(CreditStatus.CREDIT_HOLD);
        verify(dealerRepository).save(dealer);
        verify(emailService).sendCreditHoldAlert(eq("acc_manager@phucanh.vn"), eq("DL-001"), eq("Dai Ly A"),
                eq("INV-202606-000100"), eq(45L));
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

        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
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

        when(dealerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(dealer));
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(invoice));
        when(paymentReceiptRepository.findByDealerIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> paymentReceiptService.createPaymentReceipt(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Payment amount exceeds invoice remaining balance");
    }
}
