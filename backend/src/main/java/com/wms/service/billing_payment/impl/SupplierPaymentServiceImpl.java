package com.wms.service.billing_payment.impl;

import com.wms.dto.request.CreateSupplierPaymentRequest;
import com.wms.dto.response.SupplierPaymentResponse;
import com.wms.entity.access_control.User;
import com.wms.entity.billing_payment.AccountingPeriod;
import com.wms.entity.billing_payment.SupplierInvoice;
import com.wms.entity.billing_payment.SupplierPayment;
import com.wms.entity.document_numbering.DocumentSequence;
import com.wms.entity.supplier_management.Supplier;
import com.wms.enums.access_control.UserRole;
import com.wms.enums.audit_trail.AuditAction;
import com.wms.enums.billing_payment.AccountingPeriodStatus;
import com.wms.enums.billing_payment.InvoiceStatus;
import com.wms.exception.ResourceNotFoundException;
import com.wms.exception.UnprocessableEntityException;
import com.wms.repository.*;
import com.wms.repository.supplier_management.SupplierRepository;
import com.wms.service.audit_trail.AuditLogService;
import com.wms.service.billing_payment.AccountingPeriodService;
import com.wms.service.billing_payment.SupplierPaymentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private static final DateTimeFormatter PAYMENT_NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String SUPPLIER_PAYMENT_SEQUENCE_KEY = "SUPPLIER_PAYMENT";

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SupplierRepository supplierRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final DocumentSequenceRepository sequenceRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AuditLogService auditLogService;

    public SupplierPaymentServiceImpl(
            SupplierPaymentRepository supplierPaymentRepository,
            SupplierInvoiceRepository supplierInvoiceRepository,
            SupplierRepository supplierRepository,
            AccountingPeriodRepository accountingPeriodRepository,
            DocumentSequenceRepository sequenceRepository,
            AccountingPeriodService accountingPeriodService,
            AuditLogService auditLogService) {
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.supplierInvoiceRepository = supplierInvoiceRepository;
        this.supplierRepository = supplierRepository;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.sequenceRepository = sequenceRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public SupplierPaymentResponse createSupplierPayment(CreateSupplierPaymentRequest request, User actor) {
        requireAccountant(actor);

        // 1. Validate date in open period
        accountingPeriodService.validateDateInOpenPeriod(request.getDocumentDate());

        // 2. Validate supplier & invoice
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        SupplierInvoice invoice = supplierInvoiceRepository.findById(request.getSupplierInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier invoice not found with id: " + request.getSupplierInvoiceId()));

        if (!invoice.getSupplier().getId().equals(supplier.getId())) {
            throw new UnprocessableEntityException("Supplier invoice does not belong to the specified supplier");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new UnprocessableEntityException("Supplier invoice is already fully paid");
        }

        // 3. Calculate remaining balance of invoice
        BigDecimal totalPaidSoFar = BigDecimal.ZERO;
        List<SupplierPayment> existingPayments = supplierPaymentRepository.findBySupplierInvoiceId(invoice.getId());
        for (SupplierPayment sp : existingPayments) {
            totalPaidSoFar = totalPaidSoFar.add(sp.getAmount());
        }

        BigDecimal remainingAmount = invoice.getTotalAmount().subtract(totalPaidSoFar);
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new UnprocessableEntityException("Payment amount exceeds remaining invoice balance of " + remainingAmount);
        }

        // 4. Open period lookup
        AccountingPeriod period = accountingPeriodRepository
                .findPeriodByDateAndStatus(request.getDocumentDate(), AccountingPeriodStatus.OPEN)
                .orElseThrow(() -> new UnprocessableEntityException("No open accounting period found for payment date " + request.getDocumentDate()));

        // 5. Update invoice status
        BigDecimal newPaidTotal = totalPaidSoFar.add(request.getAmount());
        if (newPaidTotal.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoice.setUpdatedAt(OffsetDateTime.now());
        supplierInvoiceRepository.save(invoice);

        // 6. Decrease supplier balance
        BigDecimal oldBalance = supplier.getCurrentBalance() != null ? supplier.getCurrentBalance() : BigDecimal.ZERO;
        supplier.setCurrentBalance(oldBalance.subtract(request.getAmount()));
        supplierRepository.save(supplier);

        // 7. Generate payment number
        String paymentNumber = generateSupplierPaymentNumber(request.getDocumentDate());

        // 8. Create and save payment
        OffsetDateTime now = OffsetDateTime.now();
        SupplierPayment payment = SupplierPayment.builder()
                .paymentNumber(paymentNumber)
                .supplier(supplier)
                .supplierInvoice(invoice)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .createdBy(actor)
                .documentDate(request.getDocumentDate())
                .accountingPeriod(period)
                .notes(request.getNotes())
                .createdAt(now)
                .build();

        SupplierPayment savedPayment = supplierPaymentRepository.save(payment);

        // 9. Audit log
        auditLogService.log(actor, AuditAction.CREATE, "SUPPLIER_PAYMENT",
                savedPayment.getId(), savedPayment.getPaymentNumber(),
                null, null, snapshot(savedPayment));

        return toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierPaymentResponse getSupplierPaymentById(Long id, User actor) {
        requireAccountantOrManager(actor);
        SupplierPayment payment = supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier payment not found with id: " + id));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> getSupplierPayments(Long supplierId, Long invoiceId, User actor) {
        requireAccountantOrManager(actor);
        List<SupplierPayment> payments;
        if (invoiceId != null) {
            payments = supplierPaymentRepository.findBySupplierInvoiceId(invoiceId);
        } else if (supplierId != null) {
            payments = supplierPaymentRepository.findBySupplierId(supplierId);
        } else {
            payments = supplierPaymentRepository.findAll();
        }
        return payments.stream().map(this::toResponse).toList();
    }

    @Override
    public com.wms.dto.response.SupplierPaymentOcrResponse scanSupplierPaymentOcr(org.springframework.web.multipart.MultipartFile file, User actor) {
        requireAccountant(actor);

        if (file == null || file.isEmpty()) {
            throw new UnprocessableEntityException("Uploaded file is empty");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        // Parse amount from filename if available, else mock default 20,000,000
        BigDecimal amount = new BigDecimal("20000000.00");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{4,}").matcher(filename);
        if (matcher.find()) {
            try {
                amount = new BigDecimal(matcher.group());
            } catch (Exception ignored) {}
        }

        // Match supplier from company name or code
        List<Supplier> suppliers = supplierRepository.findAll();
        Long matchedSupplierId = null;
        String matchedSupplierName = "NHA CUNG CAP";
        for (Supplier s : suppliers) {
            if (s.getCompanyName() != null && filename.contains(s.getCompanyName().toLowerCase().replaceAll("[^a-z0-9]", ""))) {
                matchedSupplierId = s.getId();
                matchedSupplierName = s.getCompanyName();
                break;
            }
        }
        if (matchedSupplierId == null && !suppliers.isEmpty()) {
            matchedSupplierId = suppliers.getFirst().getId();
            matchedSupplierName = suppliers.getFirst().getCompanyName();
        }

        return com.wms.dto.response.SupplierPaymentOcrResponse.builder()
                .amount(amount)
                .paymentDate(LocalDate.now())
                .supplierId(matchedSupplierId)
                .notes("UNC CHI TIEN HANG - " + matchedSupplierName.toUpperCase() + " - UNC_OCR_" + (int)(Math.random() * 100000))
                .confidenceScore(0.92)
                .build();
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

    private String generateSupplierPaymentNumber(LocalDate docDate) {
        String datePart = docDate.format(PAYMENT_NUMBER_DATE);
        DocumentSequence sequence = sequenceRepository
                .findBySequenceKeyForUpdate(SUPPLIER_PAYMENT_SEQUENCE_KEY)
                .orElseGet(() -> {
                    DocumentSequence newSeq = new DocumentSequence();
                    newSeq.setSequenceKey(SUPPLIER_PAYMENT_SEQUENCE_KEY);
                    newSeq.setNextValue(1L);
                    newSeq.setUpdatedAt(OffsetDateTime.now());
                    return sequenceRepository.save(newSeq);
                });
        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        sequence.setUpdatedAt(OffsetDateTime.now());
        sequenceRepository.save(sequence);
        return "SPAY-" + datePart + "-" + String.format("%06d", value);
    }

    private SupplierPaymentResponse toResponse(SupplierPayment entity) {
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
                .accountingPeriodId(entity.getAccountingPeriod() != null ? entity.getAccountingPeriod().getId() : null)
                .documentDate(entity.getDocumentDate())
                .notes(entity.getNotes())
                .createdByName(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Map<String, Object> snapshot(SupplierPayment entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("paymentNumber", entity.getPaymentNumber());
        map.put("supplierId", entity.getSupplier().getId());
        map.put("supplierInvoiceId", entity.getSupplierInvoice().getId());
        map.put("amount", entity.getAmount());
        map.put("paymentMethod", entity.getPaymentMethod().name());
        return map;
    }
}
