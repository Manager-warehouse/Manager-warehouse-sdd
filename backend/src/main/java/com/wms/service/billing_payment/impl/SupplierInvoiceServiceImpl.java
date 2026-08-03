package com.wms.service.billing_payment.impl;

import com.wms.dto.request.CreateSupplierInvoiceRequest;
import com.wms.dto.response.SupplierInvoiceResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.stock_receiving.Receipt;
import com.wms.entity.stock_receiving.ReceiptItem;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.enums.stock_receiving.ReceiptStatus;
import com.wms.exception.BusinessRuleViolationException;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.AccountingPeriodRepository;
import com.wms.repository.DocumentSequenceRepository;
import com.wms.repository.ReceiptItemRepository;
import com.wms.repository.ReceiptRepository;
import com.wms.repository.SupplierBillingNotificationRepository;
import com.wms.repository.SupplierInvoiceRepository;
import com.wms.repository.SupplierPaymentRepository;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.SupplierInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierInvoiceServiceImpl implements SupplierInvoiceService {

    private static final DateTimeFormatter INVOICE_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String SUPPLIER_INVOICE_SEQUENCE_KEY = "SUPPLIER_INVOICE";

    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SupplierRepository supplierRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final SupplierBillingNotificationRepository supplierBillingNotificationRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AuditLogService auditLogService;
    private final SupplierPaymentRepository supplierPaymentRepository;

    public SupplierInvoiceServiceImpl(
            SupplierInvoiceRepository supplierInvoiceRepository,
            ReceiptRepository receiptRepository,
            ReceiptItemRepository receiptItemRepository,
            SupplierRepository supplierRepository,
            AccountingPeriodRepository accountingPeriodRepository,
            SupplierBillingNotificationRepository supplierBillingNotificationRepository,
            DocumentSequenceRepository sequenceRepository,
            AccountingPeriodService accountingPeriodService,
            AuditLogService auditLogService,
            SupplierPaymentRepository supplierPaymentRepository) {
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.supplierRepository = supplierRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.supplierBillingNotificationRepository = supplierBillingNotificationRepository;
        this.sequenceRepository = sequenceRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.auditLogService = auditLogService;
        this.supplierPaymentRepository = supplierPaymentRepository;
    }

    @Override
    @Transactional
    public SupplierInvoiceResponse createSupplierInvoice(CreateSupplierInvoiceRequest request, User actor) {
        requireAccountant(actor);

        // Validated up front, before any mutation (supplier balance, document sequence),
        // since it depends only on the request itself.
        LocalDate issueDate = request.getDocumentDate();
        LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : issueDate.plusDays(30);
        if (dueDate.isBefore(issueDate)) {
            throw new UnprocessableEntityException(
                    "DUE_DATE_BEFORE_DOCUMENT_DATE: Due date cannot be before the document date");
        }

        // 1. Validate date in open period
        accountingPeriodService.validateDateInOpenPeriod(request.getDocumentDate());

        // 2. Validate receipt
        Receipt receipt = receiptRepository.findById(request.getReceiptId())
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found with id: " + request.getReceiptId()));

        if (receipt.getStatus() != ReceiptStatus.PUTAWAY_COMPLETED) {
            throw new BusinessRuleViolationException("RECEIPT_NOT_PUTAWAY_COMPLETED: Receipt must be put away before creating a supplier invoice");
        }

        if (supplierInvoiceRepository.findByReceiptId(receipt.getId()).isPresent()) {
            // 409: state conflict (already invoiced), not a malformed request.
            throw new BusinessRuleViolationException("SUPPLIER_INVOICE_ALREADY_EXISTS: Supplier invoice already exists for receipt id: " + receipt.getId());
        }

        Supplier supplier = receipt.getSupplier();
        if (supplier == null) {
            throw new UnprocessableEntityException("RECEIPT_NO_SUPPLIER: Receipt does not have an associated supplier");
        }

        // Catches the realistic failure mode (double-submit, pasted the wrong invoice's
        // number) before it becomes a permanent, uneditable duplicate record.
        if (supplierInvoiceRepository.existsBySupplierIdAndSupplierInvoiceNumber(
                supplier.getId(), request.getSupplierInvoiceNumber())) {
            throw new BusinessRuleViolationException(
                    "SUPPLIER_INVOICE_NUMBER_ALREADY_USED: Supplier invoice number "
                            + request.getSupplierInvoiceNumber() + " already recorded for this supplier");
        }

        // 3. Find Open Accounting Period
        AccountingPeriod period = accountingPeriodRepository
                .findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new UnprocessableEntityException(
                        "NO_OPEN_PERIOD: No open accounting period found for date " + request.getDocumentDate()));

        // 4. Calculate total amount from approved, putaway-unlocked quantity x unit cost per line.
        BigDecimal calculatedAmount = calculateTotalAmount(receipt.getId());

        // The Accountant reconciles this calculated estimate against the supplier's actual
        // paper invoice (feature-accountant-supplier-invoicing.md 1) and may override it -
        // receipt_items.unit_cost is Planner-entered PO/expected pricing, not necessarily
        // what the supplier ultimately billed. Absent an override, the calculated amount
        // stands as before.
        BigDecimal totalAmount = request.getConfirmedTotalAmount() != null
                ? request.getConfirmedTotalAmount()
                : calculatedAmount;

        // 5. Increase supplier current balance
        BigDecimal oldBalance = supplier.getCurrentBalance() != null ? supplier.getCurrentBalance() : BigDecimal.ZERO;
        supplier.setCurrentBalance(oldBalance.add(totalAmount));
        supplierRepository.save(supplier);

        // 6. Generate invoice number
        String invoiceNumber = generateSupplierInvoiceNumber(request.getDocumentDate());

        OffsetDateTime now = OffsetDateTime.now();
        SupplierInvoice invoice = SupplierInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .supplierInvoiceNumber(request.getSupplierInvoiceNumber())
                .receipt(receipt)
                .supplier(supplier)
                .totalAmount(totalAmount)
                .calculatedAmountEstimate(calculatedAmount)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .status(InvoiceStatus.UNPAID)
                .createdBy(actor)
                .documentDate(request.getDocumentDate())
                .accountingPeriod(period)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // The findByReceiptId() check above is check-then-act, not atomic: a double-click or
        // retried request racing the same receiptId can both pass it before either commits.
        // uq_supplier_invoices_receipt_id (V33) is the real guard; translate its violation
        // into the same clean 409 the sequential (non-race) case already returns above,
        // instead of letting a raw DataIntegrityViolationException surface as a 500.
        SupplierInvoice savedInvoice;
        try {
            savedInvoice = supplierInvoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleViolationException(
                    "SUPPLIER_INVOICE_ALREADY_EXISTS: Supplier invoice already exists for receipt id: " + receipt.getId());
        }

        // 7. Update notification if exists
        supplierBillingNotificationRepository.findByReceiptId(receipt.getId()).ifPresent(notification -> {
            notification.setInvoiceStatus("INVOICED");
            notification.setStatus("ARCHIVED");
            supplierBillingNotificationRepository.save(notification);
        });

        // 8. Audit log
        auditLogService.log(actor, AuditAction.CREATE, "SUPPLIER_INVOICE",
                savedInvoice.getId(), savedInvoice.getInvoiceNumber(),
                receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null,
                null, snapshot(savedInvoice));

        return toResponse(savedInvoice);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceResponse getSupplierInvoiceById(Long id, User actor) {
        requireAccountantOrManager(actor);
        SupplierInvoice invoice = supplierInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier invoice not found with id: " + id));
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierInvoiceResponse> getSupplierInvoices(Long supplierId, String status, User actor) {
        requireAccountantOrManager(actor);
        List<SupplierInvoice> invoices;
        if (supplierId != null && status != null && !status.isEmpty()) {
            invoices = supplierInvoiceRepository.findBySupplierIdAndStatus(supplierId, InvoiceStatus.valueOf(status.toUpperCase()));
        } else if (supplierId != null) {
            invoices = supplierInvoiceRepository.findBySupplierId(supplierId);
        } else {
            invoices = supplierInvoiceRepository.findAll();
        }
        return invoices.stream().map(this::toResponse).toList();
    }

    private BigDecimal calculateTotalAmount(Long receiptId) {
        List<ReceiptItem> items = receiptItemRepository.findByReceiptId(receiptId);
        if (items.isEmpty()) {
            throw new UnprocessableEntityException("RECEIPT_NO_ITEMS: Receipt has no items to invoice");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ReceiptItem item : items) {
            if (item.getUnitCost() == null) {
                throw new UnprocessableEntityException(
                        "ITEM_UNIT_COST_MISSING: Receipt item unit cost is required for invoicing");
            }
            Integer approvedQty = item.getApprovedQty();
            if (approvedQty == null || approvedQty <= 0) {
                continue;
            }
            total = total.add(item.getUnitCost().multiply(BigDecimal.valueOf(approvedQty)));
        }
        return total;
    }

    private void requireAccountant(User actor) {
        if (actor == null || (actor.getRole() != UserRole.ACCOUNTANT && actor.getRole() != UserRole.ADMIN)) {
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

    private String generateSupplierInvoiceNumber(LocalDate docDate) {
        String datePart = docDate.format(INVOICE_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(SUPPLIER_INVOICE_SEQUENCE_KEY)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setSequenceKey(SUPPLIER_INVOICE_SEQUENCE_KEY);
                    newSeq.setNextValue(1L);
                    newSeq.setUpdatedAt(OffsetDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "SINV-" + datePart + "-" + String.format("%06d", value);
    }

    private BigDecimal calculatePaidAmount(Long supplierInvoiceId) {
        List<SupplierPayment> payments = supplierPaymentRepository.findBySupplierInvoiceId(supplierInvoiceId);
        BigDecimal total = BigDecimal.ZERO;
        for (SupplierPayment payment : payments) {
            total = total.add(payment.getAmount());
        }
        return total;
    }

    private SupplierInvoiceResponse toResponse(SupplierInvoice entity) {
        return SupplierInvoiceResponse.builder()
                .id(entity.getId())
                .invoiceNumber(entity.getInvoiceNumber())
                .supplierInvoiceNumber(entity.getSupplierInvoiceNumber())
                .receiptId(entity.getReceipt().getId())
                .receiptNumber(entity.getReceipt().getReceiptNumber())
                .supplierId(entity.getSupplier().getId())
                .supplierName(entity.getSupplier().getCompanyName())
                .totalAmount(entity.getTotalAmount())
                .calculatedAmountEstimate(entity.getCalculatedAmountEstimate())
                .paidAmount(calculatePaidAmount(entity.getId()))
                .issueDate(entity.getIssueDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .documentDate(entity.getDocumentDate())
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Map<String, Object> snapshot(SupplierInvoice entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceNumber", entity.getInvoiceNumber());
        map.put("supplierInvoiceNumber", entity.getSupplierInvoiceNumber());
        map.put("supplierId", entity.getSupplier().getId());
        map.put("totalAmount", entity.getTotalAmount());
        map.put("calculatedAmountEstimate", entity.getCalculatedAmountEstimate());
        map.put("amountOverridden", entity.getCalculatedAmountEstimate() != null
                && entity.getTotalAmount().compareTo(entity.getCalculatedAmountEstimate()) != 0);
        map.put("status", entity.getStatus().name());
        return map;
    }
}
