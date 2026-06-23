package com.wms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.entity.Batch;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.Invoice;
import com.wms.entity.InvoiceLine;
import com.wms.entity.Product;
import com.wms.entity.User;
import com.wms.entity.Warehouse;
import com.wms.enums.AuditAction;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.DeliveryOrderType;
import com.wms.enums.InvoiceStatus;
import com.wms.enums.UserRole;
import com.wms.enums.WarehouseType;
import com.wms.exception.OutboundDeliveryException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.InvoiceLineRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.service.AuditLogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class AutoInvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceLineRepository invoiceLineRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;
    @Mock private AuditLogService auditLogService;

    private AutoInvoiceServiceImpl service;
    private User actor;
    private Warehouse warehouse;
    private Dealer dealer;
    private DeliveryOrder order;

    @BeforeEach
    void setUp() {
        service = new AutoInvoiceServiceImpl(
                invoiceRepository, invoiceLineRepository, deliveryOrderItemRepository, auditLogService);
        actor = User.builder().id(10L).fullName("Accountant Bot").role(UserRole.ACCOUNTANT).build();
        warehouse = Warehouse.builder().id(20L).code("HP").name("Hai Phong")
                .type(WarehouseType.PHYSICAL).isActive(true).build();
        dealer = Dealer.builder().id(30L).name("Dealer").currentBalance(BigDecimal.valueOf(100)).build();
        order = DeliveryOrder.builder().id(40L).doNumber("DO-40").dealer(dealer).warehouse(warehouse)
                .type(DeliveryOrderType.SALE).status(DeliveryOrderStatus.IN_TRANSIT)
                .createdBy(actor).documentDate(LocalDate.now()).build();
    }

    @Test
    void createForConfirmedDelivery_createsInvoiceLinesAndIncreasesDealerBalance() {
        DeliveryOrderItem first = item(501L, BigDecimal.valueOf(2), BigDecimal.valueOf(50));
        DeliveryOrderItem second = item(502L, BigDecimal.valueOf(3), BigDecimal.valueOf(20));
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.empty());
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L)).thenReturn(List.of(first, second));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> savedInvoice(invocation.getArgument(0)));
        when(invoiceLineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AutoInvoiceResult result = service.createForConfirmedDelivery(order, actor);

        assertThat(result.totalAmount()).isEqualByComparingTo("160");
        assertThat(result.idempotentReplay()).isFalse();
        assertThat(result.lines()).hasSize(2);
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo("260");
        verify(invoiceLineRepository).saveAll(any());
        verify(auditLogService).log(eq(actor), eq(AuditAction.INVOICE_AUTO_CREATE),
                eq("INVOICE"), any(), any(), eq(20L), any(), any());
    }

    @Test
    void createForConfirmedDelivery_setsDueDateThirtyDaysAfterIssueDate() {
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.empty());
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L))
                .thenReturn(List.of(item(501L, BigDecimal.ONE, BigDecimal.TEN)));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> savedInvoice(invocation.getArgument(0)));
        when(invoiceLineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AutoInvoiceResult result = service.createForConfirmedDelivery(order, actor);

        assertThat(result.dueDate()).isEqualTo(result.issueDate().plusDays(30));
        assertThat(result.status()).isEqualTo(InvoiceStatus.UNPAID);
    }

    @Test
    void createForConfirmedDelivery_rejectsMissingUnitPrice() {
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.empty());
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L))
                .thenReturn(List.of(item(501L, BigDecimal.ONE, null)));

        assertThatThrownBy(() -> service.createForConfirmedDelivery(order, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("unit price");

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createForConfirmedDelivery_returnsExistingInvoiceWithoutBalanceMutation() {
        Invoice invoice = existingInvoice(BigDecimal.valueOf(75));
        InvoiceLine line = line(invoice, item(501L, BigDecimal.ONE, BigDecimal.valueOf(75)));
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.of(invoice));
        when(invoiceLineRepository.findByInvoiceIdOrderByIdAsc(700L)).thenReturn(List.of(line));

        AutoInvoiceResult result = service.createForConfirmedDelivery(order, actor);

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.totalAmount()).isEqualByComparingTo("75");
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo("100");
        verify(invoiceRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createForConfirmedDelivery_rejectsPartialDelivery() {
        DeliveryOrderItem partial = item(501L, BigDecimal.valueOf(2), BigDecimal.valueOf(50));
        partial.setRequestedQty(BigDecimal.valueOf(3));
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.empty());
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L)).thenReturn(List.of(partial));

        assertThatThrownBy(() -> service.createForConfirmedDelivery(order, actor))
                .isInstanceOf(OutboundDeliveryException.class)
                .hasMessageContaining("Partial delivery");
    }

    @Test
    void createForConfirmedDelivery_handlesUniqueConstraintCollisionIdempotently() {
        Invoice invoice = existingInvoice(BigDecimal.valueOf(50));
        when(invoiceRepository.findByDeliveryOrderId(40L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(invoice));
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L))
                .thenReturn(List.of(item(501L, BigDecimal.ONE, BigDecimal.valueOf(50))));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate do_id"));
        when(invoiceLineRepository.findByInvoiceIdOrderByIdAsc(700L)).thenReturn(List.of());

        AutoInvoiceResult result = service.createForConfirmedDelivery(order, actor);

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(dealer.getCurrentBalance()).isEqualByComparingTo("100");
        verify(invoiceLineRepository, never()).saveAll(any());
    }

    @Test
    void createForConfirmedDelivery_auditContainsDealerBalanceBeforeAndAfter() {
        when(invoiceRepository.findByDeliveryOrderId(40L)).thenReturn(Optional.empty());
        when(deliveryOrderItemRepository.findByDeliveryOrderId(40L))
                .thenReturn(List.of(item(501L, BigDecimal.ONE, BigDecimal.TEN)));
        when(invoiceRepository.existsByInvoiceNumber(any())).thenReturn(false);
        when(invoiceRepository.save(any())).thenAnswer(invocation -> savedInvoice(invocation.getArgument(0)));
        when(invoiceLineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createForConfirmedDelivery(order, actor);

        ArgumentCaptor<java.util.Map<String, Object>> before = ArgumentCaptor.forClass(java.util.Map.class);
        ArgumentCaptor<java.util.Map<String, Object>> after = ArgumentCaptor.forClass(java.util.Map.class);
        verify(auditLogService).log(eq(actor), eq(AuditAction.INVOICE_AUTO_CREATE),
                eq("INVOICE"), any(), any(), eq(20L), before.capture(), after.capture());
        assertThat(before.getValue()).containsEntry("dealerBalance", BigDecimal.valueOf(100));
        assertThat(after.getValue()).containsEntry("dealerBalance", BigDecimal.valueOf(110));
    }

    private Invoice savedInvoice(Invoice invoice) {
        invoice.setId(700L);
        return invoice;
    }

    private Invoice existingInvoice(BigDecimal amount) {
        return Invoice.builder()
                .id(700L)
                .invoiceNumber("INV-20260620-0001")
                .deliveryOrder(order)
                .dealer(dealer)
                .totalAmount(amount)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .status(InvoiceStatus.UNPAID)
                .createdBy(actor)
                .documentDate(LocalDate.now())
                .build();
    }

    private DeliveryOrderItem item(Long id, BigDecimal quantity, BigDecimal unitPrice) {
        Product product = Product.builder().id(id + 1000).sku("SKU-" + id).name("Pan").unit("pcs").build();
        Batch batch = Batch.builder().id(id + 2000).product(product).warehouse(warehouse).build();
        return DeliveryOrderItem.builder()
                .id(id)
                .deliveryOrder(order)
                .product(product)
                .batch(batch)
                .requestedQty(quantity)
                .issuedQty(quantity)
                .unitPrice(unitPrice)
                .build();
    }

    private InvoiceLine line(Invoice invoice, DeliveryOrderItem item) {
        return InvoiceLine.builder()
                .id(900L)
                .invoice(invoice)
                .deliveryOrderItem(item)
                .product(item.getProduct())
                .quantity(item.getIssuedQty())
                .unitPrice(item.getUnitPrice())
                .lineAmount(item.getIssuedQty().multiply(item.getUnitPrice()))
                .build();
    }
}
