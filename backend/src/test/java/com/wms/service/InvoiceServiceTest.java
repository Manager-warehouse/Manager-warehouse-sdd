package com.wms.service;

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.service.impl.InvoiceServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private DealerRepository dealerRepository;
    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private BillingNotificationRepository billingNotificationRepository;
    @Mock private DocumentSequenceRepository sequenceRepository;
    @Mock private AccountingPeriodService accountingPeriodService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private User accountantUser;
    private User storekeeperUser;
    private Dealer dealer;
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
        dealer.setCurrentBalance(BigDecimal.valueOf(10000000));
        dealer.setCreditLimit(BigDecimal.valueOf(50000000));
        dealer.setPaymentTermDays(30);
        dealer.setCreditStatus(CreditStatus.ACTIVE);

        warehouse = new Warehouse();
        warehouse.setId(5L);
        warehouse.setName("Kho HN");

        deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(20L);
        deliveryOrder.setDoNumber("DO-001");
        deliveryOrder.setStatus(DeliveryOrderStatus.DELIVERED);
        deliveryOrder.setDealer(dealer);
        deliveryOrder.setWarehouse(warehouse);

        period = new AccountingPeriod();
        period.setId(30L);
        period.setPeriodName("Kỳ 06-2026");
        period.setStartDate(LocalDate.of(2026, 6, 1));
        period.setEndDate(LocalDate.of(2026, 6, 30));
        period.setStatus(AccountingPeriodStatus.OPEN);

        sequence = new DocumentSequence();
        sequence.setSequenceKey("INVOICE");
        sequence.setNextValue(100L);
    }

    @Test
    @DisplayName("Lập hóa đơn thành công - Đầy đủ quyền, trạng thái hợp lệ")
    void createInvoice_success() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));
        request.setNotes("Notes...");

        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setId(40L);
        item.setIssuedQty(BigDecimal.valueOf(10));
        item.setUnitPrice(BigDecimal.valueOf(50000));

        when(invoiceRepository.existsByDeliveryOrderId(20L)).thenReturn(false);
        when(deliveryOrderRepository.findById(20L)).thenReturn(Optional.of(deliveryOrder));
        when(accountingPeriodRepository.findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN))
                .thenReturn(Optional.of(period));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(20L)).thenReturn(List.of(item));
        when(sequenceRepository.findBySequenceKeyForUpdate("INVOICE")).thenReturn(Optional.of(sequence));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(50L);
            return inv;
        });

        InvoiceResponse response = invoiceService.createInvoice(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-202606-000100");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(10500000));
        assertThat(deliveryOrder.getStatus()).isEqualTo(DeliveryOrderStatus.COMPLETED);

        verify(accountingPeriodService).validateDateInOpenPeriod(request.getDocumentDate());
        verify(dealerRepository).save(dealer);
        verify(deliveryOrderRepository).save(deliveryOrder);
        verify(billingNotificationRepository, times(1)).findByDeliveryOrderIdAndInvoiceStatusAndStatus(
                eq(20L),
                eq(BillingNotificationInvoiceStatus.NOT_INVOICED),
                eq(BillingNotificationStatus.ACTIVE)
        );
        verify(auditLogService).log(eq(accountantUser), eq(AuditAction.CREATE), eq("INVOICE"),
                eq(50L), eq("INV-202606-000100"), eq(5L), any(), any());
    }

    @Test
    @DisplayName("Lập hóa đơn thất bại - Quyền truy cập không hợp lệ")
    void createInvoice_deniedForNonAccountant() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        assertThatThrownBy(() -> invoiceService.createInvoice(request, storekeeperUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Accountant role required");
    }

    @Test
    @DisplayName("Lập hóa đơn thất bại - Hóa đơn cho DO đã tồn tại")
    void createInvoice_alreadyExists() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        when(invoiceRepository.existsByDeliveryOrderId(20L)).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoice(request, accountantUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Invoice already exists for this Delivery Order");
    }

    @Test
    @DisplayName("Lập hóa đơn thất bại - DO không ở trạng thái DELIVERED")
    void createInvoice_doNotDelivered() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        deliveryOrder.setStatus(DeliveryOrderStatus.PICKING);

        when(invoiceRepository.existsByDeliveryOrderId(20L)).thenReturn(false);
        when(deliveryOrderRepository.findById(20L)).thenReturn(Optional.of(deliveryOrder));

        assertThatThrownBy(() -> invoiceService.createInvoice(request, accountantUser))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Delivery Order is not in DELIVERED status");
    }
}
