package com.wms.service.impl;

import com.wms.dto.outbound.AutoInvoiceResult;
import com.wms.dto.outbound.AutoInvoiceResult.AutoInvoiceLineResult;
import com.wms.entity.Dealer;
import com.wms.entity.DeliveryOrder;
import com.wms.entity.DeliveryOrderItem;
import com.wms.entity.Invoice;
import com.wms.entity.InvoiceLine;
import com.wms.entity.User;
import com.wms.enums.AuditAction;
import com.wms.enums.DeliveryOrderStatus;
import com.wms.enums.InvoiceStatus;
import com.wms.exception.OutboundDeliveryException;
import com.wms.repository.DeliveryOrderItemRepository;
import com.wms.repository.InvoiceLineRepository;
import com.wms.repository.InvoiceRepository;
import com.wms.service.AuditLogService;
import com.wms.service.AutoInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final AuditLogService auditLogService;

    public AutoInvoiceServiceImpl(InvoiceRepository invoiceRepository,
                                  InvoiceLineRepository invoiceLineRepository,
                                  DeliveryOrderItemRepository deliveryOrderItemRepository,
                                  AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AutoInvoiceResult createForConfirmedDelivery(DeliveryOrder deliveryOrder, User actor) {
        return invoiceRepository.findByDeliveryOrderId(deliveryOrder.getId())
                .map(invoice -> toResult(invoice, true))
                .orElseGet(() -> createInvoice(deliveryOrder, actor));
    }

    private AutoInvoiceResult createInvoice(DeliveryOrder order, User actor) {
        validateOrder(order);
        Dealer dealer = order.getDealer();
        BigDecimal balanceBefore = value(dealer.getCurrentBalance());
        List<LineDraft> drafts = buildLineDrafts(order.getId());
        BigDecimal total = drafts.stream().map(LineDraft::lineAmount).reduce(ZERO, BigDecimal::add);
        LocalDate issueDate = LocalDate.now();
        Invoice invoice = newInvoice(order, actor, total, issueDate);
        try {
            Invoice saved = invoiceRepository.save(invoice);
            List<InvoiceLine> lines = drafts.stream().map(draft -> draft.toEntity(saved)).toList();
            invoiceLineRepository.saveAll(lines);
            dealer.setCurrentBalance(balanceBefore.add(total));
            dealer.setUpdatedAt(OffsetDateTime.now());
            auditInvoice(actor, saved, balanceBefore, dealer.getCurrentBalance(), lines);
            return toResult(saved, false, lines);
        } catch (DataIntegrityViolationException ex) {
            return invoiceRepository.findByDeliveryOrderId(order.getId())
                    .map(existing -> toResult(existing, true))
                    .orElseThrow(() -> ex);
        }
    }

    private void validateOrder(DeliveryOrder order) {
        if (order.getStatus() != DeliveryOrderStatus.IN_TRANSIT) {
            throw rule("DELIVERY_ORDER_STATUS_INVALID", "Delivery Order must be IN_TRANSIT before auto-invoice");
        }
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

    private Invoice newInvoice(DeliveryOrder order, User actor, BigDecimal total, LocalDate issueDate) {
        OffsetDateTime now = OffsetDateTime.now();
        return Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .deliveryOrder(order)
                .dealer(order.getDealer())
                .totalAmount(total)
                .issueDate(issueDate)
                .dueDate(issueDate.plusDays(30))
                .status(InvoiceStatus.UNPAID)
                .createdBy(actor)
                .documentDate(issueDate)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String candidate = "INV-" + date + "-" + String.format("%04d", sequence);
            if (!invoiceRepository.existsByInvoiceNumber(candidate)) {
                return candidate;
            }
        }
        throw rule("INVOICE_NUMBER_EXHAUSTED", "Cannot generate invoice number");
    }

    private void auditInvoice(User actor, Invoice invoice, BigDecimal before,
                              BigDecimal after, List<InvoiceLine> lines) {
        auditLogService.log(actor, AuditAction.INVOICE_AUTO_CREATE, "INVOICE",
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
