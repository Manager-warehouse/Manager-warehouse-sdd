package com.wms.service;

import com.wms.dto.response.PeriodSummaryResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.billing_payment.PaymentReceipt;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.dealer_management.Dealer;
import com.wms.entity.order_fulfillment.DeliveryOrder;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.price_management.PriceHistory;
import com.wms.entity.product_catalog.Product;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.supplier_management.Supplier;
import com.wms.entity.warehouse_location.Warehouse;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.billing_payment.PaymentMethod;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PaymentReceiptRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.SupplierInvoiceRepository;
import com.wms.repository.SupplierPaymentRepository;
import com.wms.service.billing_payment.impl.PeriodSummaryServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodSummaryServiceTest {

    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock private SupplierPaymentRepository supplierPaymentRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private DeliveryOrderItemRepository deliveryOrderItemRepository;

    private PeriodSummaryServiceImpl periodSummaryService;

    private User accountant;
    private AccountingPeriod period;
    private Dealer dealer;
    private Supplier supplier;
    private Invoice invoice;
    private PaymentReceipt payment;
    private SupplierInvoice supplierInvoice;
    private SupplierPayment supplierPayment;
    private PriceHistory priceHistory;

    @BeforeEach
    void setUp() {
        periodSummaryService = new PeriodSummaryServiceImpl(
                accountingPeriodRepository, invoiceRepository, paymentReceiptRepository,
                supplierInvoiceRepository, supplierPaymentRepository, priceHistoryRepository,
                deliveryOrderItemRepository);

        accountant = new User();
        accountant.setId(1L);
        accountant.setFullName("Ke Toan Vien");
        accountant.setRole(UserRole.ACCOUNTANT);

        period = new AccountingPeriod();
        period.setId(3L);
        period.setPeriodName("2026-07");
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.setStatus(AccountingPeriodStatus.OPEN);

        dealer = new Dealer();
        dealer.setId(10L);
        dealer.setName("Dai Ly A");

        supplier = new Supplier();
        supplier.setId(20L);
        supplier.setCompanyName("Cong ty Gia Dung Phung");

        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(200L);
        deliveryOrder.setDoNumber("DO-20260710-0001");

        User createdBy = new User();
        createdBy.setId(1L);
        createdBy.setFullName("Ke Toan Vien");

        invoice = new Invoice();
        invoice.setId(101L);
        invoice.setInvoiceNumber("SINV-202607-0001");
        invoice.setDeliveryOrder(deliveryOrder);
        invoice.setDealer(dealer);
        invoice.setTotalAmount(BigDecimal.valueOf(17_000_000));
        invoice.setIssueDate(LocalDate.of(2026, 7, 10));
        invoice.setDueDate(LocalDate.of(2026, 8, 9));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setDocumentDate(LocalDate.of(2026, 7, 10));
        invoice.setAccountingPeriod(period);
        invoice.setCreatedAt(OffsetDateTime.now());

        payment = new PaymentReceipt();
        payment.setId(301L);
        payment.setPaymentNumber("PMT-202607-0001");
        payment.setDealer(dealer);
        payment.setInvoice(invoice);
        payment.setAmount(BigDecimal.valueOf(5_000_000));
        payment.setPaymentDate(LocalDate.of(2026, 7, 15));
        payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        payment.setAccountingPeriod(period);
        payment.setCreatedAt(OffsetDateTime.now());

        Receipt receipt = new Receipt();
        receipt.setId(400L);
        receipt.setReceiptNumber("RN-20260705-0001");

        supplierInvoice = new SupplierInvoice();
        supplierInvoice.setId(50L);
        supplierInvoice.setInvoiceNumber("PINV-202607-0001");
        supplierInvoice.setReceipt(receipt);
        supplierInvoice.setSupplier(supplier);
        supplierInvoice.setTotalAmount(BigDecimal.valueOf(9_000_000));
        supplierInvoice.setIssueDate(LocalDate.of(2026, 7, 6));
        supplierInvoice.setDueDate(LocalDate.of(2026, 8, 5));
        supplierInvoice.setStatus(InvoiceStatus.UNPAID);
        supplierInvoice.setDocumentDate(LocalDate.of(2026, 7, 6));
        supplierInvoice.setAccountingPeriod(period);
        supplierInvoice.setCreatedAt(OffsetDateTime.now());

        supplierPayment = new SupplierPayment();
        supplierPayment.setId(501L);
        supplierPayment.setPaymentNumber("SPMT-202607-0001");
        supplierPayment.setSupplier(supplier);
        supplierPayment.setSupplierInvoice(supplierInvoice);
        supplierPayment.setAmount(BigDecimal.valueOf(4_000_000));
        supplierPayment.setPaymentDate(LocalDate.of(2026, 7, 20));
        supplierPayment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        supplierPayment.setAccountingPeriod(period);
        supplierPayment.setCreatedAt(OffsetDateTime.now());

        Product product = new Product();
        product.setId(600L);
        product.setSku("SKU-001");
        product.setName("Noi com dien");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(700L);
        warehouse.setCode("HP-01");
        warehouse.setName("Kho Hai Phong");

        priceHistory = new PriceHistory();
        priceHistory.setId(800L);
        priceHistory.setProduct(product);
        priceHistory.setWarehouse(warehouse);
        priceHistory.setEffectiveDate(LocalDate.of(2026, 7, 9));
        priceHistory.setCostPrice(BigDecimal.valueOf(100_000));
        priceHistory.setSellingPrice(BigDecimal.valueOf(150_000));
        priceHistory.setStatus(PriceHistoryStatus.APPROVED);
        priceHistory.setApprovedBy(createdBy);
        priceHistory.setApprovedAt(OffsetDateTime.now());
        priceHistory.setCreatedAt(OffsetDateTime.now());
    }

    private DeliveryOrderItem completedItem(BigDecimal unitCost, BigDecimal qcPassQty) {
        DeliveryOrderItem item = new DeliveryOrderItem();
        item.setUnitCost(unitCost);
        item.setQcPassQty(qcPassQty);
        return item;
    }

    @Test
    @DisplayName("Tong hop day du AR/AP/COGS/gia cho mot ky ke toan")
    void getPeriodSummary_happyPath_aggregatesAllSections() {
        when(accountingPeriodRepository.findById(3L)).thenReturn(Optional.of(period));
        when(invoiceRepository.findByAccountingPeriodId(3L)).thenReturn(List.of(invoice));
        when(paymentReceiptRepository.findByInvoiceId(101L)).thenReturn(List.of(payment));
        when(paymentReceiptRepository.findByAccountingPeriodIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(payment));
        when(supplierInvoiceRepository.findByAccountingPeriodId(3L)).thenReturn(List.of(supplierInvoice));
        when(supplierPaymentRepository.findBySupplierInvoiceId(50L)).thenReturn(List.of(supplierPayment));
        when(supplierPaymentRepository.findByAccountingPeriodId(3L)).thenReturn(List.of(supplierPayment));
        when(deliveryOrderItemRepository.findCompletedItemsInPeriod(any(), any()))
                .thenReturn(List.of(completedItem(BigDecimal.valueOf(60_000), BigDecimal.valueOf(10))));
        when(priceHistoryRepository.findByEffectiveDateBetweenAndStatus(
                period.getStartDate(), period.getEndDate(), PriceHistoryStatus.APPROVED))
                .thenReturn(List.of(priceHistory));

        PeriodSummaryResponse response = periodSummaryService.getPeriodSummary(3L, accountant);

        assertThat(response.getPeriodName()).isEqualTo("2026-07");
        assertThat(response.getInvoiceCount()).isEqualTo(1);
        assertThat(response.getInvoiceTotal()).isEqualByComparingTo(BigDecimal.valueOf(17_000_000));
        assertThat(response.getInvoices().get(0).getPaidAmount()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
        assertThat(response.getPaymentCount()).isEqualTo(1);
        assertThat(response.getPaymentTotal()).isEqualByComparingTo(BigDecimal.valueOf(5_000_000));
        assertThat(response.getSupplierInvoiceCount()).isEqualTo(1);
        assertThat(response.getSupplierInvoiceTotal()).isEqualByComparingTo(BigDecimal.valueOf(9_000_000));
        assertThat(response.getSupplierPaymentCount()).isEqualTo(1);
        assertThat(response.getSupplierPaymentTotal()).isEqualByComparingTo(BigDecimal.valueOf(4_000_000));
        // COGS = 60_000 * 10 = 600_000; gross margin = invoiceTotal - cogs
        assertThat(response.getCogs()).isEqualByComparingTo(BigDecimal.valueOf(600_000));
        assertThat(response.getGrossMargin()).isEqualByComparingTo(BigDecimal.valueOf(16_400_000));
        assertThat(response.getPriceChangeCount()).isEqualTo(1);
        assertThat(response.getPriceChanges().get(0).getProductSku()).isEqualTo("SKU-001");
    }

    @Test
    @DisplayName("404 khi ky ke toan khong ton tai")
    void getPeriodSummary_periodNotFound_throws() {
        when(accountingPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodSummaryService.getPeriodSummary(999L, accountant))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Tu choi - vai tro khong duoc phep xem tong hop tai chinh ky")
    void getPeriodSummary_deniedForDisallowedRole() {
        User storekeeper = new User();
        storekeeper.setId(9L);
        storekeeper.setRole(UserRole.STOREKEEPER);

        assertThatThrownBy(() -> periodSummaryService.getPeriodSummary(3L, storekeeper))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Xuat Excel tra ve noi dung khong rong cho ky co du lieu")
    void exportPeriodSummaryXlsx_returnsNonEmptyContent() {
        when(accountingPeriodRepository.findById(3L)).thenReturn(Optional.of(period));
        when(invoiceRepository.findByAccountingPeriodId(3L)).thenReturn(List.of(invoice));
        when(paymentReceiptRepository.findByInvoiceId(101L)).thenReturn(List.of());
        when(paymentReceiptRepository.findByAccountingPeriodIdOrderByCreatedAtDesc(3L)).thenReturn(List.of());
        when(supplierInvoiceRepository.findByAccountingPeriodId(3L)).thenReturn(List.of());
        when(supplierPaymentRepository.findByAccountingPeriodId(3L)).thenReturn(List.of());
        when(deliveryOrderItemRepository.findCompletedItemsInPeriod(any(), any())).thenReturn(List.of());
        when(priceHistoryRepository.findByEffectiveDateBetweenAndStatus(any(), any(), any())).thenReturn(List.of());

        byte[] content = periodSummaryService.exportPeriodSummaryXlsx(3L, accountant);

        assertThat(content).isNotEmpty();
    }
}
