package com.wms.service.billing_payment.impl;

import com.wms.dto.response.InvoiceResponse;
import com.wms.dto.response.PaymentReceiptResponse;
import com.wms.dto.response.PeriodSummaryResponse;
import com.wms.dto.response.PriceHistoryResponse;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.Invoice;
import com.wms.entity.billing_payment.PaymentReceipt;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.order_fulfillment.DeliveryOrderItem;
import com.wms.entity.price_management.PriceHistory;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.price_management.PriceHistoryStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.repository.PaymentReceiptRepository;
import com.wms.repository.PriceHistoryRepository;
import com.wms.repository.SupplierInvoiceRepository;
import com.wms.repository.SupplierPaymentRepository;
import com.wms.service.billing_payment.PeriodSummaryService;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Compiles a per-period AR/AP/COGS/pricing view - the "what actually happened
// financially this period" report that closing a period alone doesn't provide.
// Kept separate from AccountingPeriodServiceImpl (already near the ~300-line file
// guideline) rather than folded into it.
@Service
public class PeriodSummaryServiceImpl implements PeriodSummaryService {

    private final AccountingPeriodRepository accountingPeriodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;

    public PeriodSummaryServiceImpl(
            AccountingPeriodRepository accountingPeriodRepository,
            InvoiceRepository invoiceRepository,
            PaymentReceiptRepository paymentReceiptRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            SupplierPaymentRepository supplierPaymentRepository,
            PriceHistoryRepository priceHistoryRepository,
            DeliveryOrderItemRepository deliveryOrderItemRepository) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PeriodSummaryResponse getPeriodSummary(Long periodId, User actor) {
        requireAccountantOrManager(actor);
        AccountingPeriod period = accountingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found with id: " + periodId));

        List<InvoiceResponse> invoices = invoiceRepository.findByAccountingPeriodId(periodId).stream()
                .map(this::toInvoiceResponse).toList();
        BigDecimal invoiceTotal = invoices.stream().map(InvoiceResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PaymentReceiptResponse> payments = paymentReceiptRepository
                .findByAccountingPeriodIdOrderByCreatedAtDesc(periodId).stream()
                .map(this::toPaymentResponse).toList();
        BigDecimal paymentTotal = payments.stream().map(PaymentReceiptResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SupplierInvoiceResponse> supplierInvoices = supplierInvoiceRepository.findByAccountingPeriodId(periodId)
                .stream().map(this::toSupplierInvoiceResponse).toList();
        BigDecimal supplierInvoiceTotal = supplierInvoices.stream().map(SupplierInvoiceResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SupplierPaymentResponse> supplierPayments = supplierPaymentRepository.findByAccountingPeriodId(periodId)
                .stream().map(this::toSupplierPaymentResponse).toList();
        BigDecimal supplierPaymentTotal = supplierPayments.stream().map(SupplierPaymentResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cogs = computeCogs(period);

        List<PriceHistoryResponse> priceChanges = priceHistoryRepository
                .findByEffectiveDateBetweenAndStatus(period.getStartDate(), period.getEndDate(), PriceHistoryStatus.APPROVED)
                .stream().map(this::toPriceHistoryResponse).toList();

        return PeriodSummaryResponse.builder()
                .periodId(period.getId())
                .periodName(period.getPeriodName())
                .startDate(period.getStartDate())
                .endDate(period.getEndDate())
                .status(period.getStatus())
                .invoiceCount(invoices.size())
                .invoiceTotal(invoiceTotal)
                .invoices(invoices)
                .paymentCount(payments.size())
                .paymentTotal(paymentTotal)
                .payments(payments)
                .supplierInvoiceCount(supplierInvoices.size())
                .supplierInvoiceTotal(supplierInvoiceTotal)
                .supplierInvoices(supplierInvoices)
                .supplierPaymentCount(supplierPayments.size())
                .supplierPaymentTotal(supplierPaymentTotal)
                .supplierPayments(supplierPayments)
                .cogs(cogs)
                .grossMargin(invoiceTotal.subtract(cogs))
                .priceChangeCount(priceChanges.size())
                .priceChanges(priceChanges)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPeriodSummaryXlsx(Long periodId, User actor) {
        PeriodSummaryResponse summary = getPeriodSummary(periodId, actor);
        return PeriodSummaryXlsxWriter.write(summary);
    }

    // Same bounds conversion ReportServiceImpl.getCeoDashboard uses for "this calendar
    // month", applied to the period's own start/end instead of "now" - the CEO dashboard
    // figure and this one agree for the current period and stay period-accurate for past
    // closed ones.
    private BigDecimal computeCogs(AccountingPeriod period) {
        OffsetDateTime start = period.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = period.getEndDate().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
        List<DeliveryOrderItem> completedItems = deliveryOrderItemRepository.findCompletedItemsInPeriod(start, end);
        return completedItems.stream()
                .map(item -> {
                    BigDecimal unitCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
                    BigDecimal qcPass = item.getQcPassQty() != null ? item.getQcPassQty() : BigDecimal.ZERO;
                    return unitCost.multiply(qcPass);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateInvoicePaidAmount(Long invoiceId) {
        return paymentReceiptRepository.findByInvoiceId(invoiceId).stream()
                .map(PaymentReceipt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateSupplierInvoicePaidAmount(Long supplierInvoiceId) {
        return supplierPaymentRepository.findBySupplierInvoiceId(supplierInvoiceId).stream()
                .map(SupplierPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InvoiceResponse toInvoiceResponse(Invoice entity) {
        return InvoiceResponse.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .doId(entity.getDeliveryOrder().getId())
                .doNumber(entity.getDeliveryOrder().getDoNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealer().getName())
                .totalAmount(entity.getTotalAmount())
                .paidAmount(calculateInvoicePaidAmount(entity.getId()))
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .accountingPeriodName(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getPeriodName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PaymentReceiptResponse toPaymentResponse(PaymentReceipt entity) {
        return PaymentReceiptResponse.builder()
                .id(entity.getId())
                .paymentNumber(entity.getPaymentNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealer().getName())
                .invoiceId(entity.getInvoice().getId())
                .invoiceNumber(entity.getInvoice().getInvoiceNumber())
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .paymentMethod(entity.getPaymentMethod())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .accountingPeriodName(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getPeriodName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private SupplierInvoiceResponse toSupplierInvoiceResponse(SupplierInvoice entity) {
        return SupplierInvoiceResponse.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .supplierInvoiceNumber(entity.getSupplierInvoiceNumber())
                .receiptId(entity.getReceipt().getId())
                .receiptNumber(entity.getReceipt().getReceiptNumber())
                .supplierId(entity.getSupplier().getId())
                .supplierName(entity.getSupplier().getCompanyName())
                .totalAmount(entity.getTotalAmount())
                .paidAmount(calculateSupplierInvoicePaidAmount(entity.getId()))
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private SupplierPaymentResponse toSupplierPaymentResponse(SupplierPayment entity) {
        return SupplierPaymentResponse.builder()
                .id(entity.getId())
                .paymentNumber(entity.getPaymentNumber())
                .supplierId(entity.getSupplier().getId())
                .supplierName(entity.getSupplier().getCompanyName())
                .supplierInvoiceId(entity.getSupplierInvoice().getId())
                .invoiceNumber(entity.getSupplierInvoice().getInvoiceNumber())
                .amount(entity.getAmount())
                .paymentDate(entity.getPaymentDate())
                .paymentMethod(entity.getPaymentMethod())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Deliberately omits previousApproved (the delta-vs-prior-price view built for the
    // pricing approval screen) - not needed for a period-level "what prices changed" list.
    private PriceHistoryResponse toPriceHistoryResponse(PriceHistory entity) {
        return PriceHistoryResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct().getId())
                .productSku(entity.getProduct().getSku())
                .productName(entity.getProduct().getName())
                .warehouseId(entity.getWarehouse().getId())
                .warehouseName(entity.getWarehouse().getName())
                .warehouseCode(entity.getWarehouse().getCode())
                .effectiveDate(entity.getEffectiveDate())
                .costPrice(entity.getCostPrice())
                .sellingPrice(entity.getSellingPrice())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .approvedAt(entity.getApprovedAt())
                .approvedBy(entity.getApprovedBy() != null
                        ? PriceHistoryResponse.UserRef.builder()
                                .id(entity.getApprovedBy().getId())
                                .fullName(entity.getApprovedBy().getFullName())
                                .build()
                        : null)
                .build();
    }

    private void requireAccountantOrManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER
                && actor.getRole() != UserRole.ADMIN
                && actor.getRole() != UserRole.CEO)) {
            throw new AccessDeniedException("Access denied: Accountant or Accountant Manager privileges required");
        }
    }
}
