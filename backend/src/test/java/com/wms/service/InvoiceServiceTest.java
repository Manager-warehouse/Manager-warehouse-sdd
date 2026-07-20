package com.wms.service;

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.ResourceNotFoundException;
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
