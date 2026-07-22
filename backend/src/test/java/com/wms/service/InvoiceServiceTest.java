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

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.*;
import com.wms.service.billing_payment.impl.InvoiceServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * InvoiceServiceImpl is now a thin manual-backfill wrapper (POST /api/v1/invoices):
 * it only checks the actor role, loads the Delivery Order, and delegates the actual
 * credit-check / period-stamping / numbering / billing-notification logic to
 * AutoInvoiceService.createBackfillInvoice — that logic's own preconditions
 * (DELIVERY_ORDER_NOT_DELIVERED, INVOICE_ALREADY_EXISTS) are covered in
 * AutoInvoiceServiceImplTest instead.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private AutoInvoiceService autoInvoiceService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private User accountantUser;
    private User storekeeperUser;
    private Dealer dealer;
    private DeliveryOrder deliveryOrder;
    private Warehouse warehouse;

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
        dealer.setCurrentBalance(BigDecimal.valueOf(10500000));
        dealer.setCreditLimit(BigDecimal.valueOf(50000000));
        dealer.setPaymentTermDays(30);
        dealer.setCreditStatus(CreditStatus.ACTIVE);

        warehouse = new Warehouse();
        warehouse.setId(5L);
        warehouse.setName("Kho HN");

        deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(20L);
        deliveryOrder.setDoNumber("DO-001");
        deliveryOrder.setStatus(DeliveryOrderStatus.COMPLETED);
        deliveryOrder.setDealer(dealer);
        deliveryOrder.setWarehouse(warehouse);
    }

    @Test
    @DisplayName("Lập hóa đơn thành công - Đầy đủ quyền, trạng thái hợp lệ")
    void createInvoice_success() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));
        request.setNotes("Notes...");

        Invoice savedInvoice = Invoice.builder()
                .id(50L)
                .invoiceNumber("INV-202606-000100")
                .deliveryOrder(deliveryOrder)
                .dealer(dealer)
                .totalAmount(BigDecimal.valueOf(500000))
                .issueDate(request.getDocumentDate())
                .dueDate(request.getDocumentDate().plusDays(30))
                .status(InvoiceStatus.UNPAID)
                .createdBy(accountantUser)
                .documentDate(request.getDocumentDate())
                .build();

        when(deliveryOrderRepository.findById(20L)).thenReturn(Optional.of(deliveryOrder));
        when(autoInvoiceService.createBackfillInvoice(deliveryOrder, accountantUser, request.getDocumentDate()))
                .thenReturn(savedInvoice);

        InvoiceResponse response = invoiceService.createInvoice(request, accountantUser);

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).isEqualTo("INV-202606-000100");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
        verify(autoInvoiceService).createBackfillInvoice(deliveryOrder, accountantUser, request.getDocumentDate());
    }

    @Test
    @DisplayName("Lập hóa đơn thành công - Kèm bằng chứng POD từ lần giao hàng gần nhất")
    void createInvoice_includesPodEvidenceFromDelivery() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        Invoice savedInvoice = Invoice.builder()
                .id(50L)
                .invoiceNumber("INV-202606-000100")
                .deliveryOrder(deliveryOrder)
                .dealer(dealer)
                .totalAmount(BigDecimal.valueOf(500000))
                .issueDate(request.getDocumentDate())
                .dueDate(request.getDocumentDate().plusDays(30))
                .status(InvoiceStatus.UNPAID)
                .createdBy(accountantUser)
                .documentDate(request.getDocumentDate())
                .build();

        java.time.OffsetDateTime deliveredAt = java.time.OffsetDateTime.parse("2026-06-15T10:00:00Z");
        Delivery delivery = Delivery.builder()
                .deliveryOrder(deliveryOrder)
                .deliveredAt(deliveredAt)
                .podImageUrl("https://storage/pod/photo.jpg")
                .podSignatureUrl("https://storage/pod/sig.png")
                .podTimestamp(deliveredAt)
                .build();

        when(deliveryOrderRepository.findById(20L)).thenReturn(Optional.of(deliveryOrder));
        when(autoInvoiceService.createBackfillInvoice(deliveryOrder, accountantUser, request.getDocumentDate()))
                .thenReturn(savedInvoice);
        when(deliveryRepository.findFirstByDeliveryOrderIdOrderByCreatedAtDesc(20L))
                .thenReturn(Optional.of(delivery));

        InvoiceResponse response = invoiceService.createInvoice(request, accountantUser);

        assertThat(response.getOtpVerifiedAt()).isEqualTo(deliveredAt);
        assertThat(response.getPodImageUrl()).isEqualTo("https://storage/pod/photo.jpg");
        assertThat(response.getPodSignatureUrl()).isEqualTo("https://storage/pod/sig.png");
        assertThat(response.getPodTimestamp()).isEqualTo(deliveredAt);
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

        verifyNoInteractions(autoInvoiceService);
    }

    @Test
    @DisplayName("Lập hóa đơn thất bại - Không tìm thấy đơn xuất kho")
    void createInvoice_deliveryOrderNotFound() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setDoId(20L);
        request.setDocumentDate(LocalDate.of(2026, 6, 15));

        when(deliveryOrderRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(request, accountantUser))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(autoInvoiceService);
    }
}
