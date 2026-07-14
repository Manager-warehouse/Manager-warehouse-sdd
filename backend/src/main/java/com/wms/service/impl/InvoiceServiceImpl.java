package com.wms.service.impl;

import com.wms.dto.request.InvoiceCreateRequest;
import com.wms.dto.response.InvoiceResponse;
import com.wms.entity.*;
import com.wms.enums.*;
import com.wms.exception.DuplicateResourceException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.service.AccountingPeriodService;
import com.wms.service.AuditLogService;
import com.wms.service.InvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final DateTimeFormatter INVOICE_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String INVOICE_SEQUENCE_KEY = "INVOICE";

    private final InvoiceRepository invoiceRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryOrderItemRepository deliveryOrderItemRepository;
    private final DealerRepository dealerRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final BillingNotificationRepository billingNotificationRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AuditLogService auditLogService;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            DeliveryOrderRepository deliveryOrderRepository,
            DeliveryOrderItemRepository deliveryOrderItemRepository,
            DealerRepository dealerRepository,
            AccountingPeriodRepository accountingPeriodRepository,
            BillingNotificationRepository billingNotificationRepository,
            DocumentSequenceRepository sequenceRepository,
            AccountingPeriodService accountingPeriodService,
            AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.deliveryOrderItemRepository = deliveryOrderItemRepository;
        this.dealerRepository = dealerRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.billingNotificationRepository = billingNotificationRepository;
        this.sequenceRepository = sequenceRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request, User actor) {
        requireAccountant(actor);
        
        // 1. Kiểm tra xem ngày hạch toán có nằm trong kỳ kế toán đang mở không
        accountingPeriodService.validateDateInOpenPeriod(request.getDocumentDate());

        // 2. Kiểm tra xem DO đã được lập hóa đơn chưa
        if (invoiceRepository.existsByDeliveryOrderId(request.getDoId())) {
            throw new DuplicateResourceException("Invoice already exists for this Delivery Order");
        }

        // 3. Tìm kiếm DO và kiểm tra trạng thái
        DeliveryOrder deliveryOrder = deliveryOrderRepository.findById(request.getDoId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery Order not found with id: " + request.getDoId()));
        
        if (deliveryOrder.getStatus() != DeliveryOrderStatus.DELIVERED) {
            throw new UnprocessableEntityException("Delivery Order is not in DELIVERED status");
        }

        // 4. Tìm kỳ kế toán cho ngày hạch toán
        AccountingPeriod period = accountingPeriodRepository
                .findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new UnprocessableEntityException("No open accounting period found for document date"));

        // 5. Tính toán tổng tiền hóa đơn dựa trên số lượng giao thực tế (issuedQty) và đơn giá
        List<DeliveryOrderItem> items = deliveryOrderItemRepository.findByDeliveryOrderId(deliveryOrder.getId());
        if (items.isEmpty()) {
            throw new UnprocessableEntityException("Delivery Order has no items");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DeliveryOrderItem item : items) {
            BigDecimal qty = item.getIssuedQty() != null ? item.getIssuedQty() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(qty.multiply(price));
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new UnprocessableEntityException("Invoice total amount must be greater than zero");
        }

        // 6. Tăng dư nợ đại lý và cập nhật trạng thái tín dụng
        Dealer dealer = deliveryOrder.getDealer();
        BigDecimal oldBalance = dealer.getCurrentBalance() != null ? dealer.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.add(totalAmount);
        dealer.setCurrentBalance(newBalance);

        BigDecimal creditLimit = dealer.getCreditLimit() != null ? dealer.getCreditLimit() : BigDecimal.ZERO;
        if (newBalance.compareTo(creditLimit) > 0) {
            dealer.setCreditStatus(CreditStatus.CREDIT_HOLD);
        }
        dealerRepository.save(dealer);

        // 7. Sinh mã hóa đơn tự động
        String invoiceNumber = generateInvoiceNumber(request.getDocumentDate());

        // 8. Tạo hóa đơn
        OffsetDateTime now = OffsetDateTime.now();
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .deliveryOrder(deliveryOrder)
                .dealer(dealer)
                .totalAmount(totalAmount)
                .issueDate(request.getDocumentDate()) // Ngày phát hành trùng ngày chứng từ
                .dueDate(request.getDocumentDate().plusDays(dealer.getPaymentTermDays() != null ? dealer.getPaymentTermDays() : 30))
                .status(InvoiceStatus.UNPAID)
                .createdBy(actor)
                .documentDate(request.getDocumentDate())
                .accountingPeriod(period)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 9. Cập nhật đơn xuất kho sang COMPLETED
        deliveryOrder.setStatus(DeliveryOrderStatus.COMPLETED);
        deliveryOrder.setUpdatedAt(now);
        deliveryOrderRepository.save(deliveryOrder);

        // 10. Cập nhật billing notification tương ứng sang INVOICED và ARCHIVED
        billingNotificationRepository.findByDeliveryOrderIdAndInvoiceStatusAndStatus(
                deliveryOrder.getId(),
                BillingNotificationInvoiceStatus.NOT_INVOICED,
                BillingNotificationStatus.ACTIVE
        ).ifPresent(notification -> {
            notification.setInvoiceStatus(BillingNotificationInvoiceStatus.INVOICED);
            notification.setStatus(BillingNotificationStatus.ARCHIVED);
            billingNotificationRepository.save(notification);
        });

        // 11. Ghi log audit
        auditLogService.log(actor, AuditAction.CREATE, "INVOICE",
                savedInvoice.getId(), savedInvoice.getInvoiceNumber(),
                deliveryOrder.getWarehouse().getId(), null, snapshot(savedInvoice));

        return toResponse(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id, User actor) {
        requireAccountantOrManager(actor);
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoices(Long dealerId, String status, User actor) {
        requireAccountantOrManager(actor);
        List<Invoice> invoices;
        if (dealerId != null) {
            invoices = invoiceRepository.findByDealerIdOrderByCreatedAtDesc(dealerId);
        } else if (status != null && !status.isEmpty()) {
            invoices = invoiceRepository.findByStatusOrderByCreatedAtDesc(InvoiceStatus.valueOf(status.toUpperCase()));
        } else {
            invoices = invoiceRepository.findAll();
        }
        return invoices.stream().map(this::toResponse).toList();
    }

    private void requireAccountant(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ADMIN)) {
            throw new AccessDeniedException("Access denied: Accountant role required");
        }
    }

    private void requireAccountantOrManager(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT 
                && actor.getRole() != UserRole.ACCOUNTANT_MANAGER 
                && actor.getRole() != UserRole.ADMIN 
                && actor.getRole() != UserRole.CEO)) {
            throw new AccessDeniedException("Access denied: Accountant or Accountant Manager privileges required");
        }
    }

    private String generateInvoiceNumber(LocalDate docDate) {
        String datePart = docDate.format(INVOICE_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(INVOICE_SEQUENCE_KEY)
                .orElseThrow(() -> new IllegalStateException("Invoice sequence is not configured"));
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "INV-" + datePart + "-" + String.format("%06d", value);
    }

    private InvoiceResponse toResponse(Invoice entity) {
        return InvoiceResponse.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .doId(entity.getDeliveryOrder().getId())
                .doNumber(entity.getDeliveryOrder().getDoNumber())
                .dealerId(entity.getDealer().getId())
                .dealerName(entity.getDealer().getName())
                .totalAmount(entity.getTotalAmount())
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .createdById(entity.getCreatedBy().getId())
                .createdByName(entity.getCreatedBy().getFullName())
                .documentDate(entity.getDocumentDate())
                .accountingPeriodId(entity.getAccountingPeriod().getId())
                .accountingPeriodName(entity.getAccountingPeriod().getPeriodName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Map<String, Object> snapshot(Invoice entity) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("invoiceNumber", entity.getInvoiceNumber());
        values.put("doId", entity.getDeliveryOrder().getId());
        values.put("dealerId", entity.getDealer().getId());
        values.put("totalAmount", entity.getTotalAmount());
        values.put("status", entity.getStatus().name());
        return values;
    }
}
