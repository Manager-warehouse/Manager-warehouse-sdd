package com.wms.service.impl;

import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.dto.outbound.AutoInvoiceResult.AutoInvoiceLineResult;
import com.wms.entity.AccountingPeriod;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.DocumentSequence;
import com.wms.entity.Invoice;
import com.wms.entity.InvoiceLine;
import com.wms.entity.User;
import com.wms.enums.AccountingPeriodStatus;
import com.wms.enums.AuditAction;
import com.wms.enums.BillingNotificationInvoiceStatus;
import com.wms.enums.BillingNotificationStatus;
import com.wms.enums.CreditStatus;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.InvoiceStatus;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.OutboundDeliveryException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.BillingNotificationRepository;
import com.wms.repository.DealerRepository;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.InvoiceLineRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.service.AccountingPeriodService;
import com.wms.service.AuditLogService;
import com.wms.service.AutoInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoInvoiceServiceImpl implements AutoInvoiceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final DateTimeFormatter INVOICE_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String INVOICE_SEQUENCE_KEY = "INVOICE";
    private static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DealerRepository dealerRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final BillingNotificationRepository billingNotificationRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final AuditLogService auditLogService;

    public AutoInvoiceServiceImpl(InvoiceRepository invoiceRepository,
                                  InvoiceLineRepository invoiceLineRepository,
                                  DeliveryOrderItemRepository deliveryOrderItemRepository,
                                  DealerRepository dealerRepository,
                                  AccountingPeriodRepository accountingPeriodRepository,
                                  AccountingPeriodService accountingPeriodService,
                                  BillingNotificationRepository billingNotificationRepository,
                                  DocumentSequenceRepository sequenceRepository,
                                  AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.dealerRepository = dealerRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.billingNotificationRepository = billingNotificationRepository;
        this.sequenceRepository = sequenceRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AutoInvoiceResult createForConfirmedDelivery(DeliveryOrder deliveryOrder, User actor) {
        return invoiceRepository.findByDeliveryOrderId(deliveryOrder.getId())
                .map(invoice -> toResult(invoice, true))
                .orElseGet(() -> createAutoInvoice(deliveryOrder, actor));
    }

    private AutoInvoiceResult createAutoInvoice(DeliveryOrder order, User actor) {
        if (order.getStatus() != DeliveryOrderStatus.IN_TRANSIT) {
            throw rule("DELIVERY_ORDER_STATUS_INVALID", "Delivery Order must be IN_TRANSIT before auto-invoice");
        }
        try {
            PersistResult result = persistInvoice(order, actor, LocalDate.now(), AuditAction.INVOICE_AUTO_CREATE);
            return toResult(result.invoice(), false, result.lines());
        } catch (DataIntegrityViolationException ex) {
            return invoiceRepository.findByDeliveryOrderId(order.getId())
                    .map(existing -> toResult(existing, true))
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional
    public Invoice createBackfillInvoice(DeliveryOrder order, User actor, LocalDate documentDate) {
        if (order.getStatus() != DeliveryOrderStatus.COMPLETED) {
            throw new UnprocessableEntityException(
                    "DELIVERY_ORDER_NOT_DELIVERED: Delivery Order has not completed OTP + POD confirmation");
        }
        if (invoiceRepository.existsByDeliveryOrderId(order.getId())) {
            throw new DuplicateResourceException(
                    "INVOICE_ALREADY_EXISTS: Invoice already exists for this Delivery Order");
        }
        return persistInvoice(order, actor, documentDate, AuditAction.CREATE).invoice();
    }

    private record PersistResult(Invoice invoice, List<InvoiceLine> lines) {
    }

    // Shared by both the automatic (confirm-delivery) path and the manual backfill path:
    // credit-limit check, accounting-period validation/stamping, DocumentSequence numbering,
    // and billing-notification archival must stay identical between the two callers.
    private PersistResult persistInvoice(DeliveryOrder order, User actor, LocalDate documentDate, AuditAction auditAction) {
        accountingPeriodService.validateDateInOpenPeriod(documentDate);
        AccountingPeriod period = accountingPeriodRepository
                .findPeriodByDateAndStatus(documentDate, AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new UnprocessableEntityException("No open accounting period found for document date"));

        List<LineDraft> drafts = buildLineDrafts(order.getId());
        BigDecimal total = drafts.stream().map(LineDraft::lineAmount).reduce(ZERO, BigDecimal::add);

        Dealer dealer = dealerRepository.findByIdForUpdate(order.getDealer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + order.getDealer().getId()));
        BigDecimal balanceBefore = value(dealer.getCurrentBalance());
        LocalDate dueDate = documentDate.plusDays(
                dealer.getPaymentTermDays() != null ? dealer.getPaymentTermDays() : DEFAULT_PAYMENT_TERM_DAYS);

        Invoice invoice = newInvoice(order, dealer, actor, total, documentDate, dueDate, period);
        Invoice saved = invoiceRepository.save(invoice);
        List<InvoiceLine> lines = drafts.stream().map(draft -> draft.toEntity(saved)).toList();
        invoiceLineRepository.saveAll(lines);

        BigDecimal newBalance = balanceBefore.add(total);
        dealer.setCurrentBalance(newBalance);
        dealer.setUpdatedAt(OffsetDateTime.now());
        BigDecimal creditLimit = value(dealer.getCreditLimit());
        if (newBalance.compareTo(creditLimit) > 0) {
            dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);
        }
        dealerRepository.save(dealer);

        archiveBillingNotification(order);
        auditInvoice(actor, auditAction, saved, balanceBefore, newBalance, lines);
        return new PersistResult(saved, lines);
    }

    private List<LineDraft> buildLineDrafts(Long deliveryOrderId) {
        List<DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderId(deliveryOrderId);
        if (items.isEmpty()) {
            throw rule("DELIVERY_ORDER_ITEMS_MISSING", "Delivery Order has no invoiceable items");
        }
        return items.stream().map(this::toLineDraft).toList();
    }

    private LineDraft toLineDraft(DeliveryOrderItem item) {
        if (item.getUnitPrice() == null) {
            throw rule("ITEM_PRICE_MISSING", "Delivery Order item unit price is required for invoicing");
        }
        BigDecimal quantity = invoiceQuantity(item);
        BigDecimal amount = quantity.multiply(item.getUnitPrice());
        return new LineDraft(item, quantity, item.getUnitPrice(), amount);
    }

    private BigDecimal invoiceQuantity(DeliveryOrderItem item) {
        BigDecimal requested = value(item.getRequestedQty());
        BigDecimal issued = value(item.getIssuedQty());
        if (issued.compareTo(ZERO) <= 0) {
            issued = requested;
        }
        if (requested.compareTo(ZERO) > 0 && issued.compareTo(requested) != 0) {
            throw rule("PARTIAL_DELIVERY_NOT_ALLOWED", "Partial delivery invoice is out of scope");
        }
        if (issued.compareTo(ZERO) <= 0) {
            throw rule("PARTIAL_DELIVERY_NOT_ALLOWED", "Invoice quantity must be positive");
        }
        return issued;
    }

    private Invoice newInvoice(DeliveryOrder order, Dealer dealer, User actor, BigDecimal total,
                                LocalDate documentDate, LocalDate dueDate, AccountingPeriod period) {
        OffsetDateTime now = OffsetDateTime.now();
        return Invoice.builder()
                .invoiceNumber(generateInvoiceNumber(documentDate))
                .deliveryOrder(order)
                .dealer(dealer)
                .totalAmount(total)
                .issueDate(documentDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.UNPAID)
                .createdBy(actor)
                .documentDate(documentDate)
                .accountingPeriod(period)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String generateInvoiceNumber(LocalDate documentDate) {
        String datePart = documentDate.format(INVOICE_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(INVOICE_SEQUENCE_KEY)
                .orElseThrow(() -> new IllegalStateException("Invoice sequence is not configured"));
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "INV-" + datePart + "-" + String.format("%06d", value);
    }

    private void archiveBillingNotification(DeliveryOrder order) {
        billingNotificationRepository.findByDeliveryOrderIdAndInvoiceStatusAndStatus(
                order.getId(),
                BillingNotificationInvoiceStatus.NOT_INVOICED,
                BillingNotificationStatus.ACTIVE
        ).ifPresent(notification -> {
            notification.setInvoiceStatus(BillingNotificationInvoiceStatus.INVOICED);
            notification.setStatus(BillingNotificationStatus.ARCHIVED);
            billingNotificationRepository.save(notification);
        });
    }

    private void auditInvoice(User actor, AuditAction action, Invoice invoice, BigDecimal before,
                              BigDecimal after, List<InvoiceLine> lines) {
        auditLogService.log(actor, action, "INVOICE",
                invoice.getId(), invoice.getInvoiceNumber(), invoice.getDeliveryOrder().getWarehouse().getId(),
                Map.of("dealerBalance", before),
                Map.of("dealerBalance", after, "invoice", invoiceSnapshot(invoice), "lines", lineSnapshots(lines)));
    }

    private AutoInvoiceResult toResult(Invoice invoice, boolean idempotent) {
        return toResult(invoice, idempotent, invoiceLineRepository.findByInvoiceIdOrderByIdAsc(invoice.getId()));
    }

    private AutoInvoiceResult toResult(Invoice invoice, boolean idempotent, List<InvoiceLine> lines) {
        return new AutoInvoiceResult(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getDeliveryOrder().getId(),
                invoice.getDealer().getId(),
                invoice.getTotalAmount(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getStatus(),
                idempotent,
                lines.stream().map(this::toLineResult).toList());
    }

    private AutoInvoiceLineResult toLineResult(InvoiceLine line) {
        return new AutoInvoiceLineResult(
                line.getDeliveryOrderItem().getId(),
                line.getProduct().getId(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineAmount());
    }

    private List<Map<String, Object>> lineSnapshots(List<InvoiceLine> lines) {
        return lines.stream().map(line -> {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("doItemId", line.getDeliveryOrderItem().getId());
            values.put("productId", line.getProduct().getId());
            values.put("quantity", line.getQuantity());
            values.put("unitPrice", line.getUnitPrice());
            values.put("lineAmount", line.getLineAmount());
            return values;
        }).toList();
    }

    private Map<String, Object> invoiceSnapshot(Invoice invoice) {
        return Map.of(
                "invoiceId", invoice.getId(),
                "deliveryOrderId", invoice.getDeliveryOrder().getId(),
                "dealerId", invoice.getDealer().getId(),
                "totalAmount", invoice.getTotalAmount(),
                "dueDate", invoice.getDueDate());
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private OutboundDeliveryException rule(String code, String message) {
        return new OutboundDeliveryException(code, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private record LineDraft(DeliveryOrderItem item,
                             BigDecimal quantity,
                             BigDecimal unitPrice,
                             BigDecimal lineAmount) {
        InvoiceLine toEntity(Invoice invoice) {
            return InvoiceLine.builder()
                    .invoice(invoice)
                    .deliveryOrderItem(item)
                    .product(item.getProduct())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .lineAmount(lineAmount)
                    .build();
        }
    }
}
